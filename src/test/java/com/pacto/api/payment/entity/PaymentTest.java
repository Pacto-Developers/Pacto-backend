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
        assertThat(payment.getRefundedAmount()).isZero();
        assertThat(payment.getRefundableAmount()).isEqualTo(10000);
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

    @Test
    void 결제_금액의_일부를_환불한다() {
        Payment payment = paidPayment(10000);

        payment.applyRefund(3000);

        assertThat(payment.getRefundedAmount()).isEqualTo(3000);
        assertThat(payment.getRefundableAmount()).isEqualTo(7000);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
    }

    @Test
    void 부분_환불_후_남은_금액을_전액_환불한다() {
        Payment payment = paidPayment(10000);
        payment.applyRefund(3000);

        payment.applyRefund(7000);

        assertThat(payment.getRefundedAmount()).isEqualTo(10000);
        assertThat(payment.getRefundableAmount()).isZero();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void 결제_금액_전체를_환불한다() {
        Payment payment = paidPayment(10000);

        payment.applyRefund(10000);

        assertThat(payment.getRefundedAmount()).isEqualTo(10000);
        assertThat(payment.getRefundableAmount()).isZero();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void 환불_금액은_0보다_커야_한다() {
        Payment payment = paidPayment(10000);

        assertThatThrownBy(() -> payment.applyRefund(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("환불 금액은 0보다 커야 합니다.");
    }

    @Test
    void 남은_환불_가능_금액을_초과할_수_없다() {
        Payment payment = paidPayment(10000);
        payment.applyRefund(3000);

        assertThatThrownBy(() -> payment.applyRefund(7001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("환불 가능 금액을 초과했습니다.");
    }

    @Test
    void PAID와_PARTIALLY_REFUNDED_상태에서만_환불할_수_있다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);

        assertThatThrownBy(() -> payment.applyRefund(3000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("환불 가능한 결제 상태가 아닙니다.");
    }

    private Payment paidPayment(int amount) {
        Payment payment = Payment.createReady(1L, "payment-1", amount);
        payment.markPaid("imp-1");
        return payment;
    }
}
