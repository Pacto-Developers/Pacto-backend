package com.pacto.api.wallet.dto;

import com.pacto.api.wallet.entity.Withdrawal;
import com.pacto.api.wallet.entity.WithdrawalStatus;
import lombok.Getter;

@Getter
public class WithdrawResponse {
    private final Long withdrawalId;
    private final int amount;
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
