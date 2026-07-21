package com.pacto.api.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "merchant_uid", nullable = false, unique = true, updatable = false)
    private String merchantUid;

    @Column(name = "imp_uid", unique = true)
    private String impUid;

    @Column(name = "amount", nullable = false, updatable = false)
    private int amount;

    @Column(name = "refunded_amount", nullable = false, columnDefinition = "integer default 0")
    private int refundedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static Payment createReady(Long userId, String merchantUid, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }

        Payment payment = new Payment();
        payment.userId = userId;
        payment.merchantUid = merchantUid;
        payment.amount = amount;
        payment.refundedAmount = 0;
        payment.status = PaymentStatus.READY;
        return payment;
    }

    public void markPaid(String impUid) {
        this.impUid = impUid;
        this.status = PaymentStatus.PAID;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markCanceled() {
        this.status = PaymentStatus.CANCELED;
    }

    public int getRefundableAmount() {
        return amount - refundedAmount;
    }

    public void applyRefund(int refundAmount) {
        if (status != PaymentStatus.PAID && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("환불 가능한 결제 상태가 아닙니다.");
        }

        if (refundAmount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }

        if (refundAmount > getRefundableAmount()) {
            throw new IllegalArgumentException("환불 가능 금액을 초과했습니다.");
        }

        this.refundedAmount += refundAmount;
        this.status = refundedAmount == amount
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
    }
}
