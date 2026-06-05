package com.pacto.api.payment.service;

import com.pacto.api.common.exception.PaymentAlreadyProcessedException;
import com.pacto.api.common.exception.PaymentNotFoundException;
import com.pacto.api.common.exception.PaymentVerificationException;
import com.pacto.api.payment.client.PortOneClient;
import com.pacto.api.payment.client.PortOnePaymentResponse;
import com.pacto.api.payment.dto.PaymentCreateRequest;
import com.pacto.api.payment.dto.PaymentResponse;
import com.pacto.api.payment.dto.PaymentVerifyRequest;
import com.pacto.api.payment.entity.Payment;
import com.pacto.api.payment.entity.PaymentStatus;
import com.pacto.api.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;

    @Transactional
    public PaymentResponse createPayment(Long userId, PaymentCreateRequest request) {
        Payment payment = Payment.createReady(userId, generateMerchantUid(), request.getAmount());
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse verifyPayment(Long userId, PaymentVerifyRequest request) {
        Payment payment = paymentRepository.findByMerchantUid(request.getMerchantUid())
                .filter(foundPayment -> foundPayment.getUserId().equals(userId))
                .orElseThrow(PaymentNotFoundException::new);

        validateReady(payment);

        PortOnePaymentResponse portOnePayment = portOneClient.getPayment(request.getImpUid());
        validatePortOnePayment(payment, request, portOnePayment);

        payment.markPaid(portOnePayment.impUid());
        return PaymentResponse.from(payment);
    }

    private void validateReady(Payment payment) {
        if (payment.getStatus() != PaymentStatus.READY) {
            throw new PaymentAlreadyProcessedException();
        }
    }

    private void validatePortOnePayment(
            Payment payment,
            PaymentVerifyRequest request,
            PortOnePaymentResponse portOnePayment
    ) {
        if (!request.getImpUid().equals(portOnePayment.impUid())) {
            throw new PaymentVerificationException("포트원 결제 번호가 일치하지 않습니다.");
        }

        if (!payment.getMerchantUid().equals(portOnePayment.merchantUid())) {
            throw new PaymentVerificationException("결제 요청 번호가 일치하지 않습니다.");
        }

        if (payment.getAmount() != portOnePayment.amount()) {
            throw new PaymentVerificationException("결제 금액이 일치하지 않습니다.");
        }

        if (!"paid".equals(portOnePayment.status())) {
            throw new PaymentVerificationException("결제가 완료되지 않았습니다.");
        }
    }

    private String generateMerchantUid() {
        return "payment_" + UUID.randomUUID();
    }
}
