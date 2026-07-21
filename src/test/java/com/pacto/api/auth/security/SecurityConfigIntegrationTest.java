package com.pacto.api.auth.security;

import com.pacto.api.auth.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtProvider jwtProvider;

    @Test
    void 광고주도_wallet_API에_접근할_수_있다() throws Exception {
        String token = jwtProvider.createToken(1L, "ADVERTISER");

        mockMvc.perform(get("/api/v1/wallets/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("advertiserOnlyEndpoints")
    void 블로거는_광고주_전용_API에_접근할_수_없다(HttpMethod method, String path) throws Exception {
        String token = jwtProvider.createToken(1L, "BLOGGER");

        mockMvc.perform(request(method, path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("bloggerOnlyEndpoints")
    void 광고주는_블로거_전용_API에_접근할_수_없다(HttpMethod method, String path) throws Exception {
        String token = jwtProvider.createToken(1L, "ADVERTISER");

        mockMvc.perform(request(method, path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private static Stream<Arguments> advertiserOnlyEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/campaigns"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/campaigns/1/close"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/campaigns/1/proceed"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/campaigns/1/cancel"),
                Arguments.of(HttpMethod.GET, "/api/v1/campaigns/1/missions"),
                Arguments.of(HttpMethod.GET, "/api/v1/payments"),
                Arguments.of(HttpMethod.GET, "/api/v1/payments/1"),
                Arguments.of(HttpMethod.POST, "/api/v1/payments"),
                Arguments.of(HttpMethod.POST, "/api/v1/payments/1/refund"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/applications/1/accept"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/applications/1/reject"),
                Arguments.of(HttpMethod.GET, "/api/v1/applications/campaign/1"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/missions/1/approve"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/missions/1/reject"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/missions/1/cancel"),
                Arguments.of(HttpMethod.GET, "/api/v1/advertiser/dashboard"),
                Arguments.of(HttpMethod.GET, "/api/v1/advertiser/campaigns/1/escrows")
        );
    }

    private static Stream<Arguments> bloggerOnlyEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/applications"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/applications/1/cancel"),
                Arguments.of(HttpMethod.GET, "/api/v1/applications/me"),
                Arguments.of(HttpMethod.GET, "/api/v1/missions/me"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/missions/1/submit"),
                Arguments.of(HttpMethod.GET, "/api/v1/escrows")
        );
    }
}
