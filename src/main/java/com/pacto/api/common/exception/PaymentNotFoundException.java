package com.pacto.api.common.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException() {
        super("결제 요청을 찾을 수 없습니다.");
    }
}
