package com.pacto.api.payment.client;

import com.pacto.api.common.exception.PortOneApiException;
import com.pacto.api.payment.config.PortOneProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
}
