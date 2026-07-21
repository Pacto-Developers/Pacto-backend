package com.pacto.api.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_refunds",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_refunds_payment_idempotency_key",
                columnNames = {"payment_id", "idempotency_key"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, updatable = false)
    private Payment payment;

    @Column(nullable = false, updatable = false)
    private int amount;

    @Column(nullable = false, length = 500, updatable = false)
    private String reason;

    @Column(name = "idempotency_key", nullable = false, length = 100, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentRefundStatus status;

    @Column(name = "portone_cancellation_id", unique = true, length = 100)
    private String portoneCancellationId;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    public static PaymentRefund create(
            Payment payment,
            int amount,
            String reason,
            String idempotencyKey
    ) {
        if (payment == null) {
            throw new IllegalArgumentException("결제 정보는 필수입니다.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("환불 사유는 필수입니다.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("멱등성 키는 필수입니다.");
        }

        PaymentRefund refund = new PaymentRefund();
        refund.payment = payment;
        refund.amount = amount;
        refund.reason = reason;
        refund.idempotencyKey = idempotencyKey;
        refund.status = PaymentRefundStatus.REQUESTED;
        return refund;
    }

    public void markSucceeded(String portoneCancellationId) {
        validateRequested();
        if (portoneCancellationId == null || portoneCancellationId.isBlank()) {
            throw new IllegalArgumentException("포트원 취소 ID는 필수입니다.");
        }

        this.portoneCancellationId = portoneCancellationId;
        this.status = PaymentRefundStatus.SUCCEEDED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String failureReason) {
        validateRequested();
        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("환불 실패 사유는 필수입니다.");
        }

        this.failureReason = failureReason;
        this.status = PaymentRefundStatus.FAILED;
        this.failedAt = LocalDateTime.now();
    }

    private void validateRequested() {
        if (status != PaymentRefundStatus.REQUESTED) {
            throw new IllegalStateException("요청 상태의 환불만 처리할 수 있습니다.");
        }
    }
}
