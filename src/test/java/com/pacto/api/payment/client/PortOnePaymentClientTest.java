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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PortOnePaymentClientTest {

    private MockRestServiceServer server;
    private PortOnePaymentClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        PortOneProperties properties = new PortOneProperties();
        properties.setBaseUrl("https://api.iamport.kr");
        properties.setApiKey("test-key");
        properties.setApiSecret("test-secret");

        client = new PortOnePaymentClient(builder, properties);
    }

    @Test
    void 포트원_accessToken을_발급받고_결제내역을_조회한다() {
        server.expect(once(), requestTo("https://api.iamport.kr/users/getToken"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "imp_key": "test-key",
                          "imp_secret": "test-secret"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "code": 0,
                          "message": null,
                          "response": {
                            "access_token": "portone-token"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("https://api.iamport.kr/payments/imp-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "portone-token"))
                .andRespond(withSuccess("""
                        {
                          "code": 0,
                          "message": null,
                          "response": {
                            "imp_uid": "imp-1",
                            "merchant_uid": "payment-1",
                            "amount": 10000,
                            "status": "paid"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        PortOnePaymentResponse response = client.getPayment("imp-1");

        assertThat(response.impUid()).isEqualTo("imp-1");
        assertThat(response.merchantUid()).isEqualTo("payment-1");
        assertThat(response.amount()).isEqualTo(10000);
        assertThat(response.status()).isEqualTo("paid");
        server.verify();
    }

    @Test
    void accessToken_발급에_실패하면_예외가_발생한다() {
        server.expect(once(), requestTo("https://api.iamport.kr/users/getToken"))
                .andRespond(withSuccess("""
                        {
                          "code": -1,
                          "message": "invalid api key",
                          "response": null
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getPayment("imp-1"))
                .isInstanceOf(PortOneApiException.class)
                .hasMessage("포트원 access token 발급에 실패했습니다.");
        server.verify();
    }
}
