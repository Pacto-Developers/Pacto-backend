package com.pacto.api.wallet.entity;

import com.pacto.api.common.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {

    @Test
    void 결제_환불액을_가용_잔액에서_차감한다() {
        Wallet wallet = Wallet.create(1L);
        ReflectionTestUtils.setField(wallet, "balance", 10000);

        wallet.deductForPaymentRefund(3000);

        assertThat(wallet.getBalance()).isEqualTo(7000);
    }

    @Test
    void 결제_환불액보다_가용_잔액이_적으면_예외를_던진다() {
        Wallet wallet = Wallet.create(1L);
        ReflectionTestUtils.setField(wallet, "balance", 2000);

        assertThatThrownBy(() -> wallet.deductForPaymentRefund(3000))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("잔액이 부족합니다.");
    }

    @Test
    void 결제_환불액은_0보다_커야_한다() {
        Wallet wallet = Wallet.create(1L);

        assertThatThrownBy(() -> wallet.deductForPaymentRefund(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("환불 금액은 0보다 커야 합니다.");
    }

    @Test
    void 잠금잔액이_부족하면_공통_잔액부족_예외를_던진다() {
        Wallet wallet = Wallet.create(1L);
        ReflectionTestUtils.setField(wallet, "lockedBalance", 10000);

        assertThatThrownBy(() -> wallet.decreaseLockedBalance(50000))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("잔액이 부족합니다.");
    }
}
