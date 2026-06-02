package com.pacto.api.payment.client;

public record PortOnePaymentResponse(
        String impUid,
        String merchantUid,
        int amount,
        String status
) {
}
