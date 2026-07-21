package com.pacto.api.common.exception;

public class PaymentRefundNotAllowedException extends RuntimeException {

    public PaymentRefundNotAllowedException() {
        super("환불할 수 없는 결제 상태입니다.");
    }
}
