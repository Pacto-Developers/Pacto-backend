package com.pacto.api.payment.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void 결제_요청은_READY_상태로_생성된다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);

        assertThat(payment.getUserId()).isEqualTo(1L);
        assertThat(payment.getMerchantUid()).isEqualTo("payment-1");
        assertThat(payment.getAmount()).isEqualTo(10000);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(payment.getImpUid()).isNull();
    }

    @Test
    void 결제_금액은_0보다_커야한다() {
        assertThatThrownBy(() -> Payment.createReady(1L, "payment-1", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결제 금액은 0보다 커야 합니다.");
    }

    @Test
    void 포트원_결제_식별자를_저장하고_PAID_상태로_변경한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);

        payment.markPaid("imp-1");

        assertThat(payment.getImpUid()).isEqualTo("imp-1");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void 실패와_취소_상태로_변경할_수_있다() {
        Payment failedPayment = Payment.createReady(1L, "payment-1", 10000);
        Payment canceledPayment = Payment.createReady(1L, "payment-2", 10000);

        failedPayment.markFailed();
        canceledPayment.markCanceled();

        assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(canceledPayment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }
}
