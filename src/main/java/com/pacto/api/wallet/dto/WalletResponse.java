package com.pacto.api.wallet.dto;

import com.pacto.api.wallet.entity.Wallet;
import lombok.Getter;

@Getter
public class WalletResponse {
    private final Long walletId;
    private final int balance;
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
