package com.pacto.api.payment.service;

import com.pacto.api.common.exception.PaymentNotFoundException;
import com.pacto.api.payment.dto.PaymentCreateRequest;
import com.pacto.api.payment.dto.PaymentResponse;
import com.pacto.api.payment.dto.PaymentVerifyRequest;
import com.pacto.api.payment.entity.Payment;
import com.pacto.api.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse createPayment(Long userId, PaymentCreateRequest request) {
        Payment payment = Payment.createReady(userId, generateMerchantUid(), request.getAmount());
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse verifyPayment(Long userId, PaymentVerifyRequest request) {
        Payment payment = paymentRepository.findByMerchantUid(request.getMerchantUid())
                .filter(foundPayment -> foundPayment.getUserId().equals(userId))
                .orElseThrow(PaymentNotFoundException::new);

        return PaymentResponse.from(payment);
    }

    private String generateMerchantUid() {
        return "payment_" + UUID.randomUUID();
    }
}
