package com.pacto.api.common.exception;

public class InvalidWithdrawalAmountException extends RuntimeException {
    public InvalidWithdrawalAmountException() {
        super("출금 금액은 10,000원 이상이어야 합니다.");
    }
}
