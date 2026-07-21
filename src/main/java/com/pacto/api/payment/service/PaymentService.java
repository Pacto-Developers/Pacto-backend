package com.pacto.api.payment.service;

import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.exception.InvalidPaymentRefundException;
import com.pacto.api.common.exception.InvalidPaymentPageRequestException;
import com.pacto.api.common.exception.PaymentAlreadyProcessedException;
import com.pacto.api.common.exception.PaymentNotFoundException;
import com.pacto.api.common.exception.PaymentRefundNotAllowedException;
import com.pacto.api.common.exception.PaymentVerificationException;
import com.pacto.api.common.exception.PortOneApiException;
import com.pacto.api.payment.client.PortOneCancelResponse;
import com.pacto.api.payment.client.PortOneClient;
import com.pacto.api.payment.client.PortOnePaymentResponse;
import com.pacto.api.payment.dto.PaymentCreateRequest;
import com.pacto.api.payment.dto.PaymentDetailResponse;
import com.pacto.api.payment.dto.PaymentRefundResponse;
import com.pacto.api.payment.dto.PaymentResponse;
import com.pacto.api.payment.entity.Payment;
import com.pacto.api.payment.entity.PaymentRefund;
import com.pacto.api.payment.entity.PaymentStatus;
import com.pacto.api.payment.repository.PaymentRefundRepository;
import com.pacto.api.payment.repository.PaymentRepository;
import com.pacto.api.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final PortOneClient portOneClient;
    private final WalletService walletService;

    @Transactional
    public PaymentResponse createPayment(Long userId, PaymentCreateRequest request) {
        Payment payment = Payment.createReady(userId, generateMerchantUid(), request.getAmount());
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getMyPayments(Long userId, int page, int size) {
        validatePageRequest(page, size);
        PageRequest pageRequest = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return PageResponse.from(
                paymentRepository.findByUserId(userId, pageRequest),
                PaymentResponse::from
        );
    }

    @Transactional(readOnly = true)
    public PaymentDetailResponse getMyPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .filter(foundPayment -> foundPayment.getUserId().equals(userId))
                .orElseThrow(PaymentNotFoundException::new);
        return PaymentDetailResponse.from(payment);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new InvalidPaymentPageRequestException();
        }
    }

    @Transactional
    public PaymentResponse confirmPaidPayment(String paymentId) {
        Payment payment = paymentRepository.findWithLockByMerchantUid(paymentId)
                .orElseThrow(PaymentNotFoundException::new);

        if (payment.getStatus() == PaymentStatus.PAID) {
            return PaymentResponse.from(payment);
        }

        validateReady(payment);

        PortOnePaymentResponse portOnePayment = portOneClient.getPayment(paymentId);
        validatePortOnePayment(payment, portOnePayment);

        payment.markPaid(portOnePayment.paymentId());
        walletService.chargeByPayment(payment.getUserId(), payment.getAmount(), payment.getPaymentId());
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentRefundResponse refundPayment(
            Long userId,
            Long paymentId,
            int amount,
            String reason,
            String idempotencyKey
    ) {
        validateRefundRequest(amount, reason, idempotencyKey);

        Payment payment = paymentRepository.findWithLockByPaymentId(paymentId)
                .filter(foundPayment -> foundPayment.getUserId().equals(userId))
                .orElseThrow(PaymentNotFoundException::new);

        PaymentRefund existingRefund = paymentRefundRepository
                .findByPayment_PaymentIdAndIdempotencyKey(paymentId, idempotencyKey)
                .orElse(null);
        if (existingRefund != null) {
            return PaymentRefundResponse.from(existingRefund);
        }

        validateRefundablePayment(payment, amount);
        int currentCancellableAmount = payment.getRefundableAmount();

        PaymentRefund refund = paymentRefundRepository.save(
                PaymentRefund.create(payment, amount, reason, idempotencyKey)
        );

        walletService.deductByPaymentRefund(userId, amount, refund.getRefundId());

        PortOneCancelResponse portOneRefund = portOneClient.cancelPayment(
                payment.getMerchantUid(),
                amount,
                currentCancellableAmount,
                reason,
                idempotencyKey
        );
        validatePortOneRefund(amount, portOneRefund);

        payment.applyRefund(amount);
        refund.markSucceeded(portOneRefund.cancellationId());
        return PaymentRefundResponse.from(refund);
    }

    private void validateReady(Payment payment) {
        if (payment.getStatus() != PaymentStatus.READY) {
            throw new PaymentAlreadyProcessedException();
        }
    }

    private void validatePortOnePayment(
            Payment payment,
            PortOnePaymentResponse portOnePayment
    ) {
        if (!payment.getMerchantUid().equals(portOnePayment.paymentId())) {
            throw new PaymentVerificationException("결제 요청 번호가 일치하지 않습니다.");
        }

        if (payment.getAmount() != portOnePayment.amount()) {
            throw new PaymentVerificationException("결제 금액이 일치하지 않습니다.");
        }

        if (!"PAID".equals(portOnePayment.status())) {
            throw new PaymentVerificationException("결제가 완료되지 않았습니다.");
        }
    }

    private void validateRefundRequest(int amount, String reason, String idempotencyKey) {
        if (amount <= 0) {
            throw new InvalidPaymentRefundException("환불 금액은 0보다 커야 합니다.");
        }
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new InvalidPaymentRefundException("환불 사유는 1자 이상 500자 이하여야 합니다.");
        }
        if (idempotencyKey == null
                || !idempotencyKey.matches("[A-Za-z0-9_-]{16,100}")) {
            throw new InvalidPaymentRefundException(
                    "멱등성 키는 영문, 숫자, -, _로 구성된 16자 이상 100자 이하여야 합니다."
            );
        }
    }

    private void validateRefundablePayment(Payment payment, int amount) {
        if (payment.getStatus() != PaymentStatus.PAID
                && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new PaymentRefundNotAllowedException();
        }
        if (amount > payment.getRefundableAmount()) {
            throw new InvalidPaymentRefundException("남은 환불 가능 금액을 초과했습니다.");
        }
    }

    private void validatePortOneRefund(int amount, PortOneCancelResponse portOneRefund) {
        if (!"SUCCEEDED".equals(portOneRefund.status())) {
            throw new PortOneApiException("포트원 결제 취소가 완료되지 않았습니다.");
        }
        if (portOneRefund.amount() != amount) {
            throw new PortOneApiException("포트원 결제 취소 금액이 일치하지 않습니다.");
        }
    }

    private String generateMerchantUid() {
        return "payment_" + UUID.randomUUID();
    }
}
