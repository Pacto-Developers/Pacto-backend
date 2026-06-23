package com.pacto.api.wallet.entity;

public enum PointHistoryReferenceType {
    CAMPAIGN,
    ESCROW,
    PAYMENT,
    WITHDRAWAL;

    public static PointHistoryReferenceType infer(PointHistoryType type) {
        return switch (type) {
            case CHARGE -> PAYMENT;
            case LOCK -> CAMPAIGN;
            case RELEASE -> ESCROW;
            case REFUND -> ESCROW;
            case WITHDRAW -> WITHDRAWAL;
        };
    }
}
