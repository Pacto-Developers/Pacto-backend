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

    @Schema(description = "출금 금액", example = "30000")
    private final int amount;

    @Schema(description = "출금 상태", example = "PENDING")
    private final WithdrawalStatus status;

    private WithdrawResponse(Long withdrawalId, int amount, WithdrawalStatus status) {
        this.withdrawalId = withdrawalId;
        this.amount = amount;
        this.status = status;
    }

    public static WithdrawResponse from(Withdrawal withdrawal) {
        return new WithdrawResponse(
                withdrawal.getWithdrawalId(), withdrawal.getAmount(), withdrawal.getStatus()
        );
    }
}
