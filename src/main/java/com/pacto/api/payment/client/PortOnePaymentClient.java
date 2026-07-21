package com.pacto.api.payment.client;

import com.pacto.api.common.exception.PortOneApiException;
import com.pacto.api.payment.config.PortOneProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class PortOnePaymentClient implements PortOneClient {

    private final RestClient.Builder restClientBuilder;
    private final PortOneProperties properties;

    @Override
    public PortOnePaymentResponse getPayment(String paymentId) {
        PortOnePaymentApiResponse response = restClient()
                .get()
                .uri("/payments/{paymentId}", paymentId)
                .header(HttpHeaders.AUTHORIZATION, "PortOne " + properties.getApiSecret())
                .retrieve()
                .body(PortOnePaymentApiResponse.class);

        if (response == null || response.id() == null || response.amount() == null) {
            throw new PortOneApiException("포트원 결제 내역 조회에 실패했습니다.");
        }

        return new PortOnePaymentResponse(
                response.id(),
                response.amount().total(),
                response.status()
        );
    }

    @Override
    public PortOneCancelResponse cancelPayment(
            String paymentId,
            int amount,
            int currentCancellableAmount,
            String reason,
            String idempotencyKey
    ) {
        PortOneCancelPaymentApiResponse response;

        try {
            response = restClient()
                    .post()
                    .uri("/payments/{paymentId}/cancel", paymentId)
                    .header(HttpHeaders.AUTHORIZATION, "PortOne " + properties.getApiSecret())
                    .header("Idempotency-Key", quoteIdempotencyKey(idempotencyKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PortOneCancelPaymentRequest(amount, currentCancellableAmount, reason))
                    .retrieve()
                    .body(PortOneCancelPaymentApiResponse.class);
        } catch (RestClientException e) {
            throw new PortOneApiException("포트원 결제 취소에 실패했습니다.");
        }

        if (response == null
                || response.cancellation() == null
                || response.cancellation().id() == null
                || response.cancellation().status() == null
                || response.cancellation().totalAmount() <= 0
                || response.cancellation().totalAmount() > Integer.MAX_VALUE) {
            throw new PortOneApiException("포트원 결제 취소 응답이 올바르지 않습니다.");
        }

        PortOnePaymentCancellation cancellation = response.cancellation();
        return new PortOneCancelResponse(
                cancellation.id(),
                (int) cancellation.totalAmount(),
                cancellation.status()
        );
    }

    private RestClient restClient() {
        return restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    private String quoteIdempotencyKey(String idempotencyKey) {
        return '"' + idempotencyKey + '"';
    }

    private record PortOnePaymentApiResponse(
            String id,
            PortOnePaymentAmount amount,
            String status
    ) {
    }

    private record PortOnePaymentAmount(
            int total
    ) {
    }

    private record PortOneCancelPaymentRequest(
            int amount,
            int currentCancellableAmount,
            String reason
    ) {
    }

    private record PortOneCancelPaymentApiResponse(
            PortOnePaymentCancellation cancellation
    ) {
    }

    private record PortOnePaymentCancellation(
            String id,
            long totalAmount,
            String status
    ) {
    }
}
