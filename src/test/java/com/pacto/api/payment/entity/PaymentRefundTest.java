package com.pacto.api.payment.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentRefundTest {

    @Test
    void 환불_요청을_REQUESTED_상태로_생성한다() {
        Payment payment = paidPayment();

        PaymentRefund refund = PaymentRefund.create(payment, 3000, "부분 환불", "refund-key-1");

        assertThat(refund.getPayment()).isEqualTo(payment);
        assertThat(refund.getAmount()).isEqualTo(3000);
        assertThat(refund.getReason()).isEqualTo("부분 환불");
        assertThat(refund.getIdempotencyKey()).isEqualTo("refund-key-1");
        assertThat(refund.getStatus()).isEqualTo(PaymentRefundStatus.REQUESTED);
    }

    @Test
    void 환불_성공_정보를_기록한다() {
        PaymentRefund refund = refund();

        refund.markSucceeded("cancellation-1");

        assertThat(refund.getStatus()).isEqualTo(PaymentRefundStatus.SUCCEEDED);
        assertThat(refund.getPortoneCancellationId()).isEqualTo("cancellation-1");
        assertThat(refund.getCompletedAt()).isNotNull();
    }

    @Test
    void 환불_실패_정보를_기록한다() {
        PaymentRefund refund = refund();

        refund.markFailed("포트원 취소 요청 실패");

        assertThat(refund.getStatus()).isEqualTo(PaymentRefundStatus.FAILED);
        assertThat(refund.getFailureReason()).isEqualTo("포트원 취소 요청 실패");
        assertThat(refund.getFailedAt()).isNotNull();
    }

    @Test
    void 완료된_환불은_다시_처리할_수_없다() {
        PaymentRefund refund = refund();
        refund.markSucceeded("cancellation-1");

        assertThatThrownBy(() -> refund.markFailed("재처리"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("요청 상태의 환불만 처리할 수 있습니다.");
    }

    @Test
    void 환불_요청의_필수값을_검증한다() {
        Payment payment = paidPayment();

        assertThatThrownBy(() -> PaymentRefund.create(payment, 0, "부분 환불", "refund-key-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("환불 금액은 0보다 커야 합니다.");
        assertThatThrownBy(() -> PaymentRefund.create(payment, 3000, " ", "refund-key-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("환불 사유는 필수입니다.");
        assertThatThrownBy(() -> PaymentRefund.create(payment, 3000, "부분 환불", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("멱등성 키는 필수입니다.");
    }

    private PaymentRefund refund() {
        return PaymentRefund.create(paidPayment(), 3000, "부분 환불", "refund-key-1");
    }

    private Payment paidPayment() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        payment.markPaid("imp-1");
        return payment;
    }
}
