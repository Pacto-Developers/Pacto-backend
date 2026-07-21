package com.pacto.api.payment.dto;

import com.pacto.api.payment.entity.Payment;
import com.pacto.api.payment.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "결제 응답")
public class PaymentResponse {

    @Schema(description = "결제 ID", example = "1")
    private final Long paymentId;

    @Schema(description = "사용자 ID", example = "1")
    private final Long userId;

    @Schema(description = "서버에서 생성한 결제 요청 번호", example = "payment_8d2f6c7a")
    private final String merchantUid;

    @Schema(description = "포트원 결제 고유 번호", example = "imp_1234567890")
    private final String impUid;

    @Schema(description = "결제 금액", example = "10000")
    private final int amount;

    @Schema(description = "누적 환불 금액", example = "3000")
    private final int refundedAmount;

    @Schema(description = "남은 환불 가능 금액", example = "7000")
    private final int refundableAmount;

    @Schema(description = "결제 상태 기준 환불 가능 여부", example = "true")
    private final boolean refundAvailable;

    @Schema(description = "결제 상태", example = "READY")
    private final PaymentStatus status;

    @Schema(description = "생성 시각", example = "2026-06-02T13:30:00")
    private final LocalDateTime createdAt;

    private PaymentResponse(Long paymentId, Long userId, String merchantUid, String impUid,
                            int amount, int refundedAmount, int refundableAmount,
                            boolean refundAvailable, PaymentStatus status, LocalDateTime createdAt) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.merchantUid = merchantUid;
        this.impUid = impUid;
        this.amount = amount;
        this.refundedAmount = refundedAmount;
        this.refundableAmount = refundableAmount;
        this.refundAvailable = refundAvailable;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getUserId(),
                payment.getMerchantUid(),
                payment.getImpUid(),
                payment.getAmount(),
                payment.getRefundedAmount(),
                payment.getRefundableAmount(),
                isRefundAvailable(payment),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }

    private static boolean isRefundAvailable(Payment payment) {
        return (payment.getStatus() == PaymentStatus.PAID
                || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED)
                && payment.getRefundableAmount() > 0;
    }
}
