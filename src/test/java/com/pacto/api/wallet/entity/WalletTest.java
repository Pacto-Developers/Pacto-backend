package com.pacto.api.wallet.entity;

import com.pacto.api.common.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {

    @Test
    void 잠금잔액이_부족하면_공통_잔액부족_예외를_던진다() {
        Wallet wallet = Wallet.create(1L);
        ReflectionTestUtils.setField(wallet, "lockedBalance", 10000);

        assertThatThrownBy(() -> wallet.decreaseLockedBalance(50000))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("잔액이 부족합니다.");
    }
}
