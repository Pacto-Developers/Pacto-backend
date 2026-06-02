package com.pacto.api.wallet.dto;

import com.pacto.api.wallet.entity.Withdrawal;
import com.pacto.api.wallet.entity.WithdrawalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "출금 신청 응답")
public class WithdrawResponse {

    @Schema(description = "출금 신청 ID", example = "1")
    private final Long withdrawalId;

    @Schema(description = "요청 출금 금액", example = "30000")
    private final int requestedAmount;

    @Schema(description = "출금 신청 후 남은 사용 가능 잔액", example = "20000")
    private final int remainingBalance;

    @Schema(description = "출금 상태", example = "PENDING")
    private final WithdrawalStatus status;

    private WithdrawResponse(Long withdrawalId, int requestedAmount, int remainingBalance, WithdrawalStatus status) {
        this.withdrawalId = withdrawalId;
        this.requestedAmount = requestedAmount;
        this.remainingBalance = remainingBalance;
        this.status = status;
    }

    public static WithdrawResponse from(Withdrawal withdrawal, int remainingBalance) {
        return new WithdrawResponse(
                withdrawal.getWithdrawalId(),
                withdrawal.getAmount(),
                remainingBalance,
                withdrawal.getStatus()
        );
    }
}
