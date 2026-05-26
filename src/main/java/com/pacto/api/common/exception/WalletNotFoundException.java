package com.pacto.api.common.exception;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException() {
        super("지갑을 찾을 수 없습니다.");
    }
}
