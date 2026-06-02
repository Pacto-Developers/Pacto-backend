package com.pacto.api.payment.repository;

import com.pacto.api.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByMerchantUid(String merchantUid);

    Optional<Payment> findByImpUid(String impUid);

    boolean existsByMerchantUid(String merchantUid);

    boolean existsByImpUid(String impUid);
}
