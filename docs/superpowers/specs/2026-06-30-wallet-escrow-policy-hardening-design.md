# Wallet/Escrow Policy Hardening Design

## Goal

Issue #75 hardens the MVP money flow owned by BE1 without changing Application or Mission behavior. The work validates existing campaign budget and escrow behavior, adds sequential refund idempotency coverage, and rejects withdrawals below 10,000 points.

## Scope

### Production changes

- Add a common `InvalidWithdrawalAmountException`.
- Reject withdrawal amounts below 10,000 before loading or mutating a wallet.
- Convert the exception to HTTP 400 through `GlobalExceptionHandler` and `CommonResponse.failure(...)`.

### Regression coverage

- Verify automatic campaign closing changes only `RECRUITING` to `CLOSED` and preserves `remainingSlots`.
- Verify campaign budget locking uses `rewardPoint * totalSlots`.
- Verify selection creates an escrow without another wallet deduction.
- Verify unused campaign budget is refunded once when called repeatedly in sequence.
- Verify no refund occurs when every slot has been selected.
- Verify escrow release and cancel cannot be repeated or crossed after settlement.
- Verify withdrawal boundary values and unchanged state after validation failure.

## Existing Behavior Kept

- `CampaignScheduler` already closes expired recruiting campaigns without rejecting applications or refunding budget. Its production code remains unchanged.
- `EscrowLockService.lockCampaignBudget(...)` locks the full campaign budget and records one campaign `LOCK` history.
- `EscrowLockService.createEscrowForSelection(...)` creates a `LOCKED` escrow without changing wallet balances.
- `EscrowLockService.refundUnusedBudget(...)` calculates `rewardPoint * remainingSlots`, refunds that amount, and clears remaining slots.
- `EscrowLedger` permits release or cancel only from `LOCKED`.
- `EscrowSettlementService.release(...)` pays the blogger and decreases the advertiser's locked balance.
- `EscrowSettlementService.cancel(...)` restores the advertiser's balance from locked funds.

## Detailed Design

### Automatic close policy

The scheduler queries campaigns whose deadline has passed and whose status is `RECRUITING`. It calls `Campaign.close()` for each result. It must not load applications, reject pending applications, call `refundUnusedBudget(...)`, or clear remaining slots.

This issue adds a scheduler unit test as a policy guard. The scheduler production code is not changed in issue #75.

### Sequential unused-budget refund idempotency

`refundUnusedBudget(campaignId)` remains a no-op when the calculated unused budget is zero. The first successful call clears `remainingSlots`, so a later sequential call performs no wallet mutation and creates no additional `PointHistory`.

The test calls the service twice with the same campaign and verifies that wallet persistence and refund history creation occur once. Database-level protection for truly concurrent calls is deferred to Phase 3.

### Escrow settlement guard

The existing `EscrowLedger` state check remains the settlement authority. Once an escrow becomes `RELEASED` or `CANCELED`, any later release or cancel request throws `InvalidEscrowStateException` before wallet or history mutation.

Tests cover same-operation retries and crossed operations such as release followed by cancel.

### Minimum withdrawal amount

`WalletService` owns the internal withdrawal contract. It defines a minimum withdrawal amount of 10,000 and validates the request before repository access.

Amounts below 10,000, including zero and negative values, throw `InvalidWithdrawalAmountException` with the message `출금 금액은 10,000원 이상이어야 합니다.` Exactly 10,000 is valid when the wallet has sufficient balance.

Invalid requests do not load or mutate a wallet, create a withdrawal, or write point history. Insufficient valid amounts continue to use `InsufficientBalanceException`.

## Point History Contract

| Operation | Type | Amount sign | Reference |
| --- | --- | --- | --- |
| Campaign budget lock | `LOCK` | Negative | Campaign ID |
| Unused budget refund | `REFUND` | Positive | Campaign ID |
| Escrow release | `RELEASE` | Positive | Escrow ID |
| Escrow cancel | `REFUND` | Positive | Escrow ID |
| Withdrawal request | `WITHDRAW` | Negative | Withdrawal ID |

## Error Handling

`InvalidWithdrawalAmountException` is placed in the common exception package to follow the current project convention. `GlobalExceptionHandler` maps it to HTTP 400 and wraps the message with `CommonResponse.failure(...)`.

Existing escrow state conflicts continue to return HTTP 409 through `InvalidEscrowStateException` handling.

## Test Strategy

- Add focused Mockito unit tests for scheduler, wallet, budget refund, and escrow settlement behavior.
- Preserve existing successful lock, release, cancel, and withdrawal tests.
- Run affected test classes during implementation.
- Run `./gradlew test` before completion.

## Out of Scope

- Pessimistic locking, optimistic locking, and other concurrent request controls.
- Application pending rejection and selection policy.
- Campaign `CLOSED -> IN_PROGRESS` orchestration.
- Mission completion and revision workflows.
- Withdrawal approval, rejection, and bank-transfer administration.
- Changes to Application or Mission production files.

## Files Expected to Change

- `src/main/java/com/pacto/api/wallet/service/WalletService.java`
- `src/main/java/com/pacto/api/common/exception/InvalidWithdrawalAmountException.java`
- `src/main/java/com/pacto/api/common/exception/GlobalExceptionHandler.java`
- `src/test/java/com/pacto/api/wallet/service/WalletServiceTest.java`
- `src/test/java/com/pacto/api/escrow/service/EscrowLockServiceTest.java`
- `src/test/java/com/pacto/api/escrow/service/EscrowSettlementServiceTest.java`
- `src/test/java/com/pacto/api/campaign/scheduler/CampaignSchedulerTest.java`
