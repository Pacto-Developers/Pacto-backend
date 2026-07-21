package com.pacto.api.payment.client;

public record PortOneCancelResponse(
        String cancellationId,
        int amount,
        String status
) {
}
