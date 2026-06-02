package com.pacto.api.auth.security;

import com.pacto.api.auth.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(status().isNotFound());
    }
}
