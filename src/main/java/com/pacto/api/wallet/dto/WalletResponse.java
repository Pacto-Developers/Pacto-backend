package com.pacto.api.wallet.dto;

import com.pacto.api.wallet.entity.Wallet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "지갑 잔액 응답")
public class WalletResponse {

    @Schema(description = "지갑 ID", example = "1")
    private final Long walletId;

    @Schema(description = "사용 가능 잔액", example = "50000")
    private final int balance;

    @Schema(description = "잠금 잔액", example = "10000")
    private final int lockedBalance;

    private WalletResponse(Long walletId, int balance, int lockedBalance) {
        this.walletId = walletId;
        this.balance = balance;
        this.lockedBalance = lockedBalance;
    }

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getWalletId(), wallet.getBalance(), wallet.getLockedBalance());
    }
}
