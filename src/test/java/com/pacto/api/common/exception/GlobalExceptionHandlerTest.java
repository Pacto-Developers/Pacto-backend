package com.pacto.api.common.exception;

import com.pacto.api.escrow.exception.InvalidEscrowStateException;
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

        @GetMapping("/test/escrow-not-found")
        void throwEscrowNotFound() { throw new EscrowNotFoundException(); }

        @GetMapping("/test/duplicate-email")
        void throwDuplicateEmail() { throw new DuplicateEmailException(); }

        @GetMapping("/test/email-not-found")
        void throwEmailNotFound() { throw new EmailNotFoundException(); }

        @GetMapping("/test/invalid-password")
        void throwInvalidPassword() { throw new InvalidPasswordException(); }

        @GetMapping("/test/user-not-found")
        void throwUserNotFound() { throw new UserNotFoundException(); }

        @GetMapping("/test/runtime")
        void throwRuntime() { throw new RuntimeException("unexpected"); }

        @GetMapping("/test/invalid-escrow-state")
        void throwInvalidEscrowState() {
            throw new InvalidEscrowStateException("LOCKED 상태의 에스크로만 처리할 수 있습니다.");
        }

        @GetMapping("/test/invalid-payment-page")
        void throwInvalidPaymentPage() { throw new InvalidPaymentPageRequestException(); }

        @GetMapping("/test/invalid-withdrawal-amount")
        void throwInvalidWithdrawalAmount() { throw new InvalidWithdrawalAmountException(); }
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
    void 에스크로없음_예외는_404_반환() throws Exception {
        mockMvc.perform(get("/test/escrow-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("에스크로를 찾을 수 없습니다."));
    }

    @Test
    void 중복이메일_예외는_409_반환() throws Exception {
        mockMvc.perform(get("/test/duplicate-email"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다."));
    }

    @Test
    void 존재하지않는이메일_예외는_404_반환() throws Exception {
        mockMvc.perform(get("/test/email-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("존재하지 않는 이메일입니다."));
    }

    @Test
    void 비밀번호불일치_예외는_401_반환() throws Exception {
        mockMvc.perform(get("/test/invalid-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
    }

    @Test
    void 유저없음_예외는_404_반환() throws Exception {
        mockMvc.perform(get("/test/user-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @Test
    void RuntimeException은_500_반환() throws Exception {
        mockMvc.perform(get("/test/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }

    @Test
    void 잘못된_에스크로_상태_예외는_409_반환() throws Exception {
        mockMvc.perform(get("/test/invalid-escrow-state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("LOCKED 상태의 에스크로만 처리할 수 있습니다."));
    }

    @Test
    void 잘못된_결제_내역_페이지_요청은_400_반환() throws Exception {
        mockMvc.perform(get("/test/invalid-payment-page"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 1 이상이고, 페이지 크기는 1 이상 100 이하여야 합니다."));
    }

    @Test
    void 최소금액_미만_출금은_400_반환() throws Exception {
        mockMvc.perform(get("/test/invalid-withdrawal-amount"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("출금 금액은 10,000원 이상이어야 합니다."));
    }
}
