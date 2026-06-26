package com.pacto.api.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.response.CommonResponse;
import com.pacto.api.payment.dto.PaymentResponse;
import com.pacto.api.payment.dto.PaymentDetailResponse;
import com.pacto.api.payment.entity.Payment;
import com.pacto.api.payment.service.PaymentService;
import com.pacto.api.payment.service.PortOneWebhookVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock PaymentService paymentService;
    @Mock PortOneWebhookVerifier portOneWebhookVerifier;
    @Spy ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks PaymentController paymentController;

    @Test
    void 내_결제_내역_조회는_CommonResponse로_응답한다() {
        PageResponse<PaymentResponse> payments = PageResponse.from(new PageImpl<>(List.of()), PaymentResponse::from);
        when(paymentService.getMyPayments(1L, 1, 20)).thenReturn(payments);

        ResponseEntity<CommonResponse<PageResponse<PaymentResponse>>> response = paymentController.getMyPayments(
                new UsernamePasswordAuthenticationToken(1L, null),
                1,
                20
        );

        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("결제 내역 조회 성공");
        assertThat(response.getBody().data()).isSameAs(payments);
    }

    @Test
    void 내_결제_상세_조회는_CommonResponse로_응답한다() {
        PaymentDetailResponse payment = PaymentDetailResponse.from(Payment.createReady(1L, "payment-1", 10000));
        when(paymentService.getMyPayment(1L, 7L)).thenReturn(payment);

        ResponseEntity<CommonResponse<PaymentDetailResponse>> response = paymentController.getMyPayment(
                new UsernamePasswordAuthenticationToken(1L, null),
                7L
        );

        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("결제 상세 조회 성공");
        assertThat(response.getBody().data()).isSameAs(payment);
    }

    @Test
    void 포트원_결제완료_웹훅은_결제를_확정한다() {
        String payload = """
                {
                  "type": "Transaction.Paid",
                  "data": {
                    "paymentId": "payment-1",
                    "storeId": "store-1",
                    "transactionId": "transaction-1"
                  }
                }
                """;
        HttpHeaders headers = new HttpHeaders();

        ResponseEntity<Void> response = paymentController.handlePortOneWebhook(payload, headers);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(portOneWebhookVerifier).verify(payload, headers);
        verify(paymentService).confirmPaidPayment("payment-1");
    }

    @Test
    void 포트원_결제완료가_아닌_웹훅은_무시한다() {
        String payload = """
                {
                  "type": "Transaction.Ready",
                  "data": {
                    "paymentId": "payment-1",
                    "storeId": "store-1",
                    "transactionId": "transaction-1"
                  }
                }
                """;
        HttpHeaders headers = new HttpHeaders();

        ResponseEntity<Void> response = paymentController.handlePortOneWebhook(payload, headers);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(portOneWebhookVerifier).verify(payload, headers);
        verify(paymentService, never()).confirmPaidPayment("payment-1");
    }
}
