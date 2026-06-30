# Wallet/Escrow Policy Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve the automatic-close and escrow money-flow contracts with regression tests and reject withdrawals below 10,000 points through the common error response.

**Architecture:** Keep the existing Scheduler and Escrow production behavior unchanged and lock it down with focused Mockito tests. Add the minimum-withdrawal rule at the `WalletService` boundary before repository access, expose a dedicated common exception, and map it to HTTP 400 through the existing global advice.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring Data JPA, JUnit 5, Mockito, AssertJ, MockMvc, Gradle

---

## File Map

| Responsibility | File | Change |
| --- | --- | --- |
| Automatic-close policy test | `src/test/java/com/pacto/api/campaign/scheduler/CampaignSchedulerTest.java` | Create |
| Sequential refund idempotency tests | `src/test/java/com/pacto/api/escrow/service/EscrowLockServiceTest.java` | Modify |
| Crossed settlement guard tests | `src/test/java/com/pacto/api/escrow/service/EscrowSettlementServiceTest.java` | Modify |
| Minimum withdrawal exception | `src/main/java/com/pacto/api/common/exception/InvalidWithdrawalAmountException.java` | Create |
| Common HTTP error mapping | `src/main/java/com/pacto/api/common/exception/GlobalExceptionHandler.java` | Modify |
| Common response test | `src/test/java/com/pacto/api/common/exception/GlobalExceptionHandlerTest.java` | Modify |
| Withdrawal policy enforcement | `src/main/java/com/pacto/api/wallet/service/WalletService.java` | Modify |
| Withdrawal boundary tests | `src/test/java/com/pacto/api/wallet/service/WalletServiceTest.java` | Modify |

## Task 1: Automatic Close Policy Regression Test

**Files:**
- Create: `src/test/java/com/pacto/api/campaign/scheduler/CampaignSchedulerTest.java`
- Verify only: `src/main/java/com/pacto/api/campaign/scheduler/CampaignScheduler.java`

- [ ] **Step 1: Add the scheduler characterization tests**

Create `CampaignSchedulerTest.java` with the complete test class below:

```java
package com.pacto.api.campaign.scheduler;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.repository.CampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignSchedulerTest {

    @Mock CampaignRepository campaignRepository;
    @InjectMocks CampaignScheduler campaignScheduler;

    @Test
    void 자동_마감은_RECRUITING을_CLOSED로_변경하고_슬롯을_유지한다() {
        Campaign campaign = new Campaign(
                1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now().minusMinutes(1), 3
        );
        when(campaignRepository.findByDeadlineBeforeAndStatus(any(LocalDateTime.class), eq(CampaignStatus.RECRUITING)))
                .thenReturn(List.of(campaign));

        campaignScheduler.closeExpiredCampaigns();

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CLOSED);
        assertThat(campaign.getRemainingSlots()).isEqualTo(3);
        verify(campaignRepository).findByDeadlineBeforeAndStatus(
                any(LocalDateTime.class), eq(CampaignStatus.RECRUITING)
        );
        verifyNoMoreInteractions(campaignRepository);
    }

    @Test
    void 자동_마감_대상이_없으면_변경_없이_종료한다() {
        when(campaignRepository.findByDeadlineBeforeAndStatus(any(LocalDateTime.class), eq(CampaignStatus.RECRUITING)))
                .thenReturn(List.of());

        campaignScheduler.closeExpiredCampaigns();

        verify(campaignRepository).findByDeadlineBeforeAndStatus(
                any(LocalDateTime.class), eq(CampaignStatus.RECRUITING)
        );
        verifyNoMoreInteractions(campaignRepository);
    }
}
```

- [ ] **Step 2: Run the scheduler test**

Run:

```bash
./gradlew test --tests com.pacto.api.campaign.scheduler.CampaignSchedulerTest
```

Expected: `BUILD SUCCESSFUL`. These are characterization tests for behavior already merged by the Scheduler owner.

- [ ] **Step 3: Commit the scheduler regression test**

```bash
git add src/test/java/com/pacto/api/campaign/scheduler/CampaignSchedulerTest.java
git commit -m "test: 캠페인 자동 마감 정책 회귀 테스트 추가"
```

## Task 2: Sequential Refund Idempotency

**Files:**
- Modify: `src/test/java/com/pacto/api/escrow/service/EscrowLockServiceTest.java`
- Verify only: `src/main/java/com/pacto/api/escrow/service/EscrowLockService.java`

- [ ] **Step 1: Add Mockito imports for invocation counts**

Add these static imports:

```java
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
```

- [ ] **Step 2: Add the repeated-refund test**

Append this test method:

```java
@Test
void refundUnusedBudget은_순차_재호출되어도_한_번만_환불한다() {
    Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
    ReflectionTestUtils.setField(campaign, "campaignId", 10L);
    campaign.decreaseSlot();
    Wallet advertiserWallet = Wallet.create(1L);
    ReflectionTestUtils.setField(advertiserWallet, "walletId", 100L);
    ReflectionTestUtils.setField(advertiserWallet, "lockedBalance", 150000);

    when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
    when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(advertiserWallet));

    escrowLockService.refundUnusedBudget(10L);
    escrowLockService.refundUnusedBudget(10L);

    assertThat(campaign.getRemainingSlots()).isZero();
    assertThat(advertiserWallet.getBalance()).isEqualTo(100000);
    assertThat(advertiserWallet.getLockedBalance()).isEqualTo(50000);
    verify(walletRepository, times(1)).findByUserId(1L);
    verify(walletRepository, times(1)).save(advertiserWallet);
    verify(campaignRepository, times(1)).save(campaign);
    verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
}
```

- [ ] **Step 3: Add the fully-selected no-refund test**

Append this test method:

```java
@Test
void refundUnusedBudget은_모든_슬롯이_선정되면_환불하지_않는다() {
    Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
    campaign.decreaseSlot();
    campaign.decreaseSlot();
    campaign.decreaseSlot();
    when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

    escrowLockService.refundUnusedBudget(10L);

    assertThat(campaign.getRemainingSlots()).isZero();
    verifyNoInteractions(walletRepository, pointHistoryRepository);
    verify(campaignRepository, never()).save(any(Campaign.class));
}
```

- [ ] **Step 4: Run the refund tests**

Run:

```bash
./gradlew test --tests com.pacto.api.escrow.service.EscrowLockServiceTest
```

Expected: `BUILD SUCCESSFUL`. The existing zero-budget early return should satisfy both new cases without production changes.

## Task 3: Crossed Escrow Settlement Guards

**Files:**
- Modify: `src/test/java/com/pacto/api/escrow/service/EscrowSettlementServiceTest.java`
- Verify only: `src/main/java/com/pacto/api/escrow/entity/EscrowLedger.java`

- [ ] **Step 1: Add release-then-cancel coverage**

Append this test:

```java
@Test
void cancel은_이미_RELEASED인_에스크로를_처리하지_않는다() {
    EscrowLedger escrow = EscrowLedger.create(10L, 42L, 50000);
    escrow.release();
    when(escrowLedgerRepository.findById(505L)).thenReturn(Optional.of(escrow));

    assertThatThrownBy(() -> escrowSettlementService.cancel(505L))
            .isInstanceOf(InvalidEscrowStateException.class)
            .hasMessage("LOCKED 상태의 에스크로만 처리할 수 있습니다.");

    verify(escrowLedgerRepository, never()).save(any());
    verifyNoInteractions(campaignRepository, walletRepository, pointHistoryRepository);
}
```

- [ ] **Step 2: Add cancel-then-release coverage**

Append this test:

```java
@Test
void release는_이미_CANCELED인_에스크로를_처리하지_않는다() {
    EscrowLedger escrow = EscrowLedger.create(10L, 42L, 50000);
    escrow.cancel();
    when(escrowLedgerRepository.findById(505L)).thenReturn(Optional.of(escrow));

    assertThatThrownBy(() -> escrowSettlementService.release(505L))
            .isInstanceOf(InvalidEscrowStateException.class)
            .hasMessage("LOCKED 상태의 에스크로만 처리할 수 있습니다.");

    verify(escrowLedgerRepository, never()).save(any());
    verifyNoInteractions(campaignRepository, walletRepository, pointHistoryRepository);
}
```

- [ ] **Step 3: Run settlement tests**

Run:

```bash
./gradlew test --tests com.pacto.api.escrow.service.EscrowSettlementServiceTest
```

Expected: `BUILD SUCCESSFUL` because `EscrowLedger.validateLocked()` rejects both crossed transitions.

- [ ] **Step 4: Commit escrow policy tests**

```bash
git add src/test/java/com/pacto/api/escrow/service/EscrowLockServiceTest.java src/test/java/com/pacto/api/escrow/service/EscrowSettlementServiceTest.java
git commit -m "test: 에스크로 환불 정산 멱등성 검증"
```

## Task 4: Minimum Withdrawal Amount

**Files:**
- Create: `src/main/java/com/pacto/api/common/exception/InvalidWithdrawalAmountException.java`
- Modify: `src/main/java/com/pacto/api/common/exception/GlobalExceptionHandler.java`
- Modify: `src/test/java/com/pacto/api/common/exception/GlobalExceptionHandlerTest.java`
- Modify: `src/main/java/com/pacto/api/wallet/service/WalletService.java`
- Modify: `src/test/java/com/pacto/api/wallet/service/WalletServiceTest.java`

- [ ] **Step 1: Write failing wallet boundary tests**

Add these imports to `WalletServiceTest`:

```java
import com.pacto.api.common.exception.InvalidWithdrawalAmountException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
```

Append these tests:

```java
@ParameterizedTest
@ValueSource(ints = {-10000, 0, 9999})
void 출금신청은_10000원_미만이면_거부한다(int amount) {
    WithdrawRequest request = new WithdrawRequest();
    ReflectionTestUtils.setField(request, "amount", amount);
    ReflectionTestUtils.setField(request, "bankName", "카카오뱅크");
    ReflectionTestUtils.setField(request, "accountNumber", "111");

    assertThatThrownBy(() -> walletService.requestWithdraw(1L, request))
            .isInstanceOf(InvalidWithdrawalAmountException.class)
            .hasMessage("출금 금액은 10,000원 이상이어야 합니다.");

    verifyNoInteractions(walletRepository, withdrawalRepository, pointHistoryRepository);
}

@Test
void 출금신청은_정확히_10000원이면_허용한다() {
    ReflectionTestUtils.setField(wallet, "balance", 10000);
    Withdrawal savedWithdrawal = Withdrawal.create(wallet, 10000, "카카오뱅크", "111");
    ReflectionTestUtils.setField(savedWithdrawal, "withdrawalId", 1L);
    when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
    when(withdrawalRepository.save(any(Withdrawal.class))).thenReturn(savedWithdrawal);

    WithdrawRequest request = new WithdrawRequest();
    ReflectionTestUtils.setField(request, "amount", 10000);
    ReflectionTestUtils.setField(request, "bankName", "카카오뱅크");
    ReflectionTestUtils.setField(request, "accountNumber", "111");

    WithdrawResponse response = walletService.requestWithdraw(1L, request);

    assertThat(response.getRequestedAmount()).isEqualTo(10000);
    assertThat(response.getRemainingBalance()).isZero();
    verify(walletRepository).save(wallet);
    verify(withdrawalRepository).save(any(Withdrawal.class));
    verify(pointHistoryRepository).save(any(PointHistory.class));
}
```

- [ ] **Step 2: Write the failing common-response test**

Add this endpoint to `GlobalExceptionHandlerTest.FakeController`:

```java
@GetMapping("/test/invalid-withdrawal-amount")
void throwInvalidWithdrawalAmount() { throw new InvalidWithdrawalAmountException(); }
```

Append this test:

```java
@Test
void 최소금액_미만_출금은_400_반환() throws Exception {
    mockMvc.perform(get("/test/invalid-withdrawal-amount"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("출금 금액은 10,000원 이상이어야 합니다."));
}
```

- [ ] **Step 3: Run tests and confirm RED**

Run:

```bash
./gradlew test --tests com.pacto.api.wallet.service.WalletServiceTest --tests com.pacto.api.common.exception.GlobalExceptionHandlerTest
```

Expected: test compilation fails because `InvalidWithdrawalAmountException` does not exist yet.

- [ ] **Step 4: Create the exception**

Create `InvalidWithdrawalAmountException.java`:

```java
package com.pacto.api.common.exception;

public class InvalidWithdrawalAmountException extends RuntimeException {
    public InvalidWithdrawalAmountException() {
        super("출금 금액은 10,000원 이상이어야 합니다.");
    }
}
```

- [ ] **Step 5: Map the exception to the common response**

Add this method to `GlobalExceptionHandler` near the other wallet exceptions:

```java
@ExceptionHandler(InvalidWithdrawalAmountException.class)
public ResponseEntity<?> handleInvalidWithdrawalAmount(InvalidWithdrawalAmountException e) {
    return ResponseEntity.badRequest()
            .body(CommonResponse.failure(e.getMessage()));
}
```

- [ ] **Step 6: Enforce the service boundary**

Add the import and constant to `WalletService`:

```java
import com.pacto.api.common.exception.InvalidWithdrawalAmountException;

private static final int MIN_WITHDRAWAL_AMOUNT = 10_000;
```

Add this check as the first statement in `requestWithdraw(...)`, before `walletRepository.findByUserId(...)`:

```java
if (request.getAmount() < MIN_WITHDRAWAL_AMOUNT) {
    throw new InvalidWithdrawalAmountException();
}
```

- [ ] **Step 7: Run wallet and common exception tests and confirm GREEN**

Run:

```bash
./gradlew test --tests com.pacto.api.wallet.service.WalletServiceTest --tests com.pacto.api.common.exception.GlobalExceptionHandlerTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit minimum withdrawal validation**

```bash
git add src/main/java/com/pacto/api/common/exception/InvalidWithdrawalAmountException.java src/main/java/com/pacto/api/common/exception/GlobalExceptionHandler.java src/main/java/com/pacto/api/wallet/service/WalletService.java src/test/java/com/pacto/api/common/exception/GlobalExceptionHandlerTest.java src/test/java/com/pacto/api/wallet/service/WalletServiceTest.java
git commit -m "fix: 최소 출금 금액 검증 추가"
```

## Task 5: Full Verification

**Files:**
- Verify: all changed files

- [ ] **Step 1: Check formatting and unresolved markers**

Run:

```bash
git diff --check
rg -n "<<<<<<<|=======|>>>>>>>" src/main/java src/test/java
```

Expected: both commands produce no error output.

- [ ] **Step 2: Run the complete test suite**

Run:

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL` with zero failed tests.

- [ ] **Step 3: Review final scope**

Run:

```bash
git status --short --branch
git log --oneline --decorate -5
```

Expected: only issue #75 files and the design/plan documentation are present; no Application or Mission production file is changed.
