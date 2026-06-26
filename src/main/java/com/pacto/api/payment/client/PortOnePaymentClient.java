package com.pacto.api.payment.client;

import com.pacto.api.common.exception.PortOneApiException;
import com.pacto.api.payment.config.PortOneProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
                response.id(),
                response.amount().total(),
                response.status()
        );
    }

    private RestClient restClient() {
        return restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
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
}
