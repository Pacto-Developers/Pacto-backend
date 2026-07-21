package com.pacto.api.payment.entity;

public enum PaymentStatus {
    READY,
    PAID,
    PARTIALLY_REFUNDED,
    REFUNDED,
    FAILED,
    CANCELED
}
