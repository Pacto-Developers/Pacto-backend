package com.pacto.api.common.exception;

public class InvalidPaymentRefundException extends RuntimeException {

    public InvalidPaymentRefundException(String message) {
        super(message);
    }
}
