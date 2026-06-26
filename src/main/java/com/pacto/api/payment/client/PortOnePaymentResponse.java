package com.pacto.api.payment.client;

public record PortOnePaymentResponse(
        String paymentId,
        int amount,
        String status
) {
}
