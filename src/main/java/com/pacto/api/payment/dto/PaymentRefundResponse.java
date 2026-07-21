package com.pacto.api.payment.dto;

import com.pacto.api.payment.entity.Payment;
import com.pacto.api.payment.entity.PaymentRefund;
import com.pacto.api.payment.entity.PaymentRefundStatus;
import com.pacto.api.payment.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "결제 환불 응답")
public class PaymentRefundResponse {

    @Schema(description = "환불 ID", example = "15")
    private final Long refundId;

    @Schema(description = "결제 ID", example = "7")
    private final Long paymentId;

    @Schema(description = "이번 환불 금액", example = "5000")
    private final int refundAmount;

    @Schema(description = "원 결제 금액", example = "30000")
    private final int paymentAmount;

    @Schema(description = "누적 환불 금액", example = "10000")
    private final int refundedAmount;

    @Schema(description = "남은 환불 가능 금액", example = "20000")
    private final int refundableAmount;

    @Schema(description = "결제 상태", example = "PARTIALLY_REFUNDED")
    private final PaymentStatus paymentStatus;

    @Schema(description = "환불 처리 상태", example = "SUCCEEDED")
    private final PaymentRefundStatus refundStatus;

    @Schema(description = "포트원 취소 ID", example = "cancellation-1")
    private final String portoneCancellationId;

    @Schema(description = "환불 요청 시각", example = "2026-07-21T15:30:00")
    private final LocalDateTime createdAt;

    @Schema(description = "환불 완료 시각", example = "2026-07-21T15:30:03")
    private final LocalDateTime completedAt;

    private PaymentRefundResponse(
            Long refundId,
            Long paymentId,
            int refundAmount,
            int paymentAmount,
            int refundedAmount,
            int refundableAmount,
            PaymentStatus paymentStatus,
            PaymentRefundStatus refundStatus,
            String portoneCancellationId,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.refundAmount = refundAmount;
        this.paymentAmount = paymentAmount;
        this.refundedAmount = refundedAmount;
        this.refundableAmount = refundableAmount;
        this.paymentStatus = paymentStatus;
        this.refundStatus = refundStatus;
        this.portoneCancellationId = portoneCancellationId;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public static PaymentRefundResponse from(PaymentRefund refund) {
        Payment payment = refund.getPayment();
        return new PaymentRefundResponse(
                refund.getRefundId(),
                payment.getPaymentId(),
                refund.getAmount(),
                payment.getAmount(),
                payment.getRefundedAmount(),
                payment.getRefundableAmount(),
                payment.getStatus(),
                refund.getStatus(),
                refund.getPortoneCancellationId(),
                refund.getCreatedAt(),
                refund.getCompletedAt()
        );
    }
}
