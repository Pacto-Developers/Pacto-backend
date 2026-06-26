package com.pacto.api.payment.client;

public interface PortOneClient {

    PortOnePaymentResponse getPayment(String paymentId);
}
