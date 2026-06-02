package com.pacto.api.payment.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pacto.api.common.exception.PortOneApiException;
import com.pacto.api.payment.config.PortOneProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PortOnePaymentClient implements PortOneClient {

    private final RestClient.Builder restClientBuilder;
    private final PortOneProperties properties;

    @Override
    public PortOnePaymentResponse getPayment(String impUid) {
        String accessToken = getAccessToken();
        PortOnePaymentApiResponse response = restClient()
                .get()
                .uri("/payments/{impUid}", impUid)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .retrieve()
                .body(PortOnePaymentApiResponse.class);

        if (response == null || response.code() != 0 || response.response() == null) {
            throw new PortOneApiException("포트원 결제 내역 조회에 실패했습니다.");
        }

        PortOnePaymentAnnotation payment = response.response();
        return new PortOnePaymentResponse(
                payment.impUid(),
                payment.merchantUid(),
                payment.amount(),
                payment.status()
        );
    }

    private String getAccessToken() {
        PortOneTokenResponse response = restClient()
                .post()
                .uri("/users/getToken")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PortOneTokenRequest(properties.getApiKey(), properties.getApiSecret()))
                .retrieve()
                .body(PortOneTokenResponse.class);

        if (response == null || response.code() != 0 || response.response() == null) {
            throw new PortOneApiException("포트원 access token 발급에 실패했습니다.");
        }

        return response.response().accessToken();
    }

    private RestClient restClient() {
        return restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    private record PortOneTokenRequest(
            @JsonProperty("imp_key")
            String apiKey,

            @JsonProperty("imp_secret")
            String apiSecret
    ) {
    }

    private record PortOneTokenResponse(
            int code,
            String message,
            PortOneTokenAnnotation response
    ) {
    }

    private record PortOneTokenAnnotation(
            @JsonProperty("access_token")
            String accessToken
    ) {
    }

    private record PortOnePaymentApiResponse(
            int code,
            String message,
            PortOnePaymentAnnotation response
    ) {
    }

    private record PortOnePaymentAnnotation(
            @JsonProperty("imp_uid")
            String impUid,

            @JsonProperty("merchant_uid")
            String merchantUid,

            int amount,
            String status
    ) {
    }
}
