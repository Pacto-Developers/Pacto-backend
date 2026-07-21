package com.pacto.api.payment.client;

public interface PortOneClient {

    PortOnePaymentResponse getPayment(String paymentId);

    PortOneCancelResponse cancelPayment(
            String paymentId,
            int amount,
            int currentCancellableAmount,
            String reason,
            String idempotencyKey
    );
}
