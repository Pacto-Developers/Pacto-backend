package com.pacto.api.payment.repository;

import com.pacto.api.payment.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {

    Optional<PaymentRefund> findByPayment_PaymentIdAndIdempotencyKey(
            Long paymentId,
            String idempotencyKey
    );

    List<PaymentRefund> findByPayment_PaymentIdOrderByCreatedAtDesc(Long paymentId);
}
