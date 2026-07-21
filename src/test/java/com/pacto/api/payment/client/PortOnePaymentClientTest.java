package com.pacto.api.payment.client;

import com.pacto.api.common.exception.PortOneApiException;
import com.pacto.api.payment.config.PortOneProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class PortOnePaymentClientTest {

    private MockRestServiceServer server;
    private PortOnePaymentClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        PortOneProperties properties = new PortOneProperties();
        properties.setBaseUrl("https://api.portone.io");
        properties.setApiKey("test-key");
        properties.setApiSecret("test-secret");

        client = new PortOnePaymentClient(builder, properties);
    }

    @Test
    void 포트원_V2_결제내역을_조회한다() {
        server.expect(once(), requestTo("https://api.portone.io/payments/payment-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "PortOne test-secret"))
                .andRespond(withSuccess("""
                        {
                          "id": "payment-1",
                          "transactionId": "transaction-1",
                          "amount": {
                            "total": 10000
                          },
                          "status": "PAID"
                        }
                        """, MediaType.APPLICATION_JSON));

        PortOnePaymentResponse response = client.getPayment("payment-1");

        assertThat(response.paymentId()).isEqualTo("payment-1");
        assertThat(response.amount()).isEqualTo(10000);
        assertThat(response.status()).isEqualTo("PAID");
        server.verify();
    }

    @Test
    void 결제내역_조회_응답이_비어있으면_예외가_발생한다() {
        server.expect(once(), requestTo("https://api.portone.io/payments/payment-1"))
                .andRespond(withSuccess("""
                        {
                          "id": null,
                          "amount": null,
                          "status": null
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getPayment("payment-1"))
                .isInstanceOf(PortOneApiException.class)
                .hasMessage("포트원 결제 내역 조회에 실패했습니다.");
        server.verify();
    }

    @Test
    void 포트원_V2_결제를_부분_취소한다() {
        server.expect(once(), requestTo("https://api.portone.io/payments/payment-1/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "PortOne test-secret"))
                .andExpect(header("Idempotency-Key", "\"refund-request-1\""))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "amount": 3000,
                          "currentCancellableAmount": 10000,
                          "reason": "고객 요청"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "cancellation": {
                            "status": "SUCCEEDED",
                            "id": "cancellation-1",
                            "pgCancellationId": "pg-cancellation-1",
                            "totalAmount": 3000,
                            "reason": "고객 요청"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        PortOneCancelResponse response = client.cancelPayment(
                "payment-1",
                3000,
                10000,
                "고객 요청",
                "refund-request-1"
        );

        assertThat(response.cancellationId()).isEqualTo("cancellation-1");
        assertThat(response.amount()).isEqualTo(3000);
        assertThat(response.status()).isEqualTo("SUCCEEDED");
        server.verify();
    }

    @Test
    void 결제_취소_API가_실패하면_예외가_발생한다() {
        server.expect(once(), requestTo("https://api.portone.io/payments/payment-1/cancel"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "type": "CANCEL_AMOUNT_EXCEEDS_CANCELLABLE_AMOUNT",
                                  "message": "취소 가능 금액을 초과했습니다."
                                }
                                """));

        assertThatThrownBy(() -> client.cancelPayment(
                "payment-1",
                3000,
                10000,
                "고객 요청",
                "refund-request-1"
        ))
                .isInstanceOf(PortOneApiException.class)
                .hasMessage("포트원 결제 취소에 실패했습니다.");
        server.verify();
    }

    @Test
    void 결제_취소_응답이_올바르지_않으면_예외가_발생한다() {
        server.expect(once(), requestTo("https://api.portone.io/payments/payment-1/cancel"))
                .andRespond(withSuccess("""
                        {
                          "cancellation": null
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.cancelPayment(
                "payment-1",
                3000,
                10000,
                "고객 요청",
                "refund-request-1"
        ))
                .isInstanceOf(PortOneApiException.class)
                .hasMessage("포트원 결제 취소 응답이 올바르지 않습니다.");
        server.verify();
    }
}
