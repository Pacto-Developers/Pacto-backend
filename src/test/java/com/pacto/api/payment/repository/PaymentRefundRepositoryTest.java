package com.pacto.api.payment.repository;

import com.pacto.api.payment.entity.Payment;
import com.pacto.api.payment.entity.PaymentRefund;
import com.pacto.api.payment.entity.PaymentRefundStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class PaymentRefundRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentRefundRepository paymentRefundRepository;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.createReady(1L, "payment-1", 10000);
        payment.markPaid("imp-1");
        payment = paymentRepository.save(payment);
    }

    @Test
    void 환불_요청을_저장하고_결제와_멱등성_키로_조회한다() {
        PaymentRefund refund = paymentRefundRepository.save(
                PaymentRefund.create(payment, 3000, "부분 환불", "refund-key-1")
        );

        var found = paymentRefundRepository.findByPayment_PaymentIdAndIdempotencyKey(
                payment.getPaymentId(),
                "refund-key-1"
        );

        assertThat(refund.getRefundId()).isNotNull();
        assertThat(refund.getCreatedAt()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualTo(3000);
        assertThat(found.get().getStatus()).isEqualTo(PaymentRefundStatus.REQUESTED);
    }

    @Test
    void 결제별_환불_요청을_최신순으로_조회한다() {
        PaymentRefund first = paymentRefundRepository.save(
                PaymentRefund.create(payment, 3000, "첫 번째 환불", "refund-key-1")
        );
        PaymentRefund second = paymentRefundRepository.save(
                PaymentRefund.create(payment, 2000, "두 번째 환불", "refund-key-2")
        );

        var refunds = paymentRefundRepository.findByPayment_PaymentIdOrderByCreatedAtDesc(
                payment.getPaymentId()
        );

        assertThat(refunds).extracting(PaymentRefund::getRefundId)
                .containsExactly(second.getRefundId(), first.getRefundId());
    }

    @Test
    void 동일_결제에_같은_멱등성_키를_중복_저장할_수_없다() {
        paymentRefundRepository.saveAndFlush(
                PaymentRefund.create(payment, 3000, "부분 환불", "refund-key-1")
        );

        assertThatThrownBy(() -> paymentRefundRepository.saveAndFlush(
                PaymentRefund.create(payment, 2000, "재요청", "refund-key-1")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
