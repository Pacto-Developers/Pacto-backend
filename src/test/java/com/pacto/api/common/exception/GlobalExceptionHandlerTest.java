package com.pacto.api.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class FakeController {
        @GetMapping("/test/insufficient")
        void throwInsufficient() { throw new InsufficientBalanceException(); }

        @GetMapping("/test/not-found")
        void throwNotFound() { throw new WalletNotFoundException(); }

        @GetMapping("/test/runtime")
        void throwRuntime() { throw new RuntimeException("unexpected"); }
    }

    @Test
    void 잔액부족_예외는_400_반환() throws Exception {
        mockMvc.perform(get("/test/insufficient"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("잔액이 부족합니다."));
    }

    @Test
    void 지갑없음_예외는_404_반환() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("지갑을 찾을 수 없습니다."));
    }

    @Test
    void RuntimeException은_500_반환() throws Exception {
        mockMvc.perform(get("/test/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }
}
