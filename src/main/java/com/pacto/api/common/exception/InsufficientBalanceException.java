package com.pacto.api.common.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("잔액이 부족합니다.");
    }
}
