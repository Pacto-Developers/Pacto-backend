package com.pacto.api.payment.service;

import com.pacto.api.common.exception.PaymentVerificationException;
import com.pacto.api.payment.config.PortOneProperties;
import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PortOneWebhookVerifier {

    private final PortOneProperties properties;

    public void verify(String payload, HttpHeaders headers) {
        if (!StringUtils.hasText(properties.getWebhookSecret())) {
            throw new PaymentVerificationException("포트원 웹훅 시크릿이 설정되지 않았습니다.");
        }

        try {
            WebhookVerifier verifier = new WebhookVerifier(properties.getWebhookSecret());
            verifier.verify(
                    payload,
                    headers.getFirst(WebhookVerifier.HEADER_ID),
                    headers.getFirst(WebhookVerifier.HEADER_SIGNATURE),
                    headers.getFirst(WebhookVerifier.HEADER_TIMESTAMP)
            );
        } catch (io.portone.sdk.server.errors.WebhookVerificationException e) {
            throw new PaymentVerificationException("포트원 웹훅 검증에 실패했습니다.");
        }
    }
}
