package com.pacto.api.common.exception;

public class InvalidChargeAmountException extends RuntimeException {
    public InvalidChargeAmountException() {
        super("충전 금액은 0보다 커야 합니다.");
    }
}
