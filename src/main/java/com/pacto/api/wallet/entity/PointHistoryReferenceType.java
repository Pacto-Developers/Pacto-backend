package com.pacto.api.wallet.entity;

public enum PointHistoryReferenceType {
    CAMPAIGN,
    ESCROW,
    PAYMENT,
    PAYMENT_REFUND,
    WITHDRAWAL;

    public static PointHistoryReferenceType infer(PointHistoryType type) {
        return switch (type) {
            case CHARGE -> PAYMENT;
            case PAYMENT_REFUND -> PAYMENT_REFUND;
            case LOCK -> CAMPAIGN;
            case RELEASE -> ESCROW;
            case REFUND -> ESCROW;
            case WITHDRAW -> WITHDRAWAL;
        };
    }
}
