package com.pacto.api.common.exception;

public class PaymentAlreadyProcessedException extends RuntimeException {
    public PaymentAlreadyProcessedException() {
        super("이미 처리된 결제입니다.");
    }
}
