package com.pacto.api.payment.repository;

import com.pacto.api.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    Optional<Payment> findByMerchantUid(String merchantUid);

    Optional<Payment> findByImpUid(String impUid);

    boolean existsByMerchantUid(String merchantUid);

    boolean existsByImpUid(String impUid);
}
