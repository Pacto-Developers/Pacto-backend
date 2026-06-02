package com.pacto.api.payment.repository;

import com.pacto.api.payment.entity.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void 결제_저장_후_merchantUid로_조회한다() {
        Payment payment = paymentRepository.save(Payment.createReady(1L, "payment-1", 10000));

        Optional<Payment> found = paymentRepository.findByMerchantUid(payment.getMerchantUid());

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
        assertThat(found.get().getAmount()).isEqualTo(10000);
    }

    @Test
    void 결제_완료_후_impUid로_조회한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        payment.markPaid("imp-1");
        paymentRepository.save(payment);

        Optional<Payment> found = paymentRepository.findByImpUid("imp-1");

        assertThat(found).isPresent();
        assertThat(found.get().getMerchantUid()).isEqualTo("payment-1");
    }

    @Test
    void merchantUid와_impUid_존재_여부를_확인한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        payment.markPaid("imp-1");
        paymentRepository.save(payment);

        assertThat(paymentRepository.existsByMerchantUid("payment-1")).isTrue();
        assertThat(paymentRepository.existsByMerchantUid("payment-2")).isFalse();
        assertThat(paymentRepository.existsByImpUid("imp-1")).isTrue();
        assertThat(paymentRepository.existsByImpUid("imp-2")).isFalse();
    }
}
