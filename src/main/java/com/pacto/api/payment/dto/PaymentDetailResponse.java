package com.pacto.api.payment.dto;

import com.pacto.api.payment.entity.Payment;
import com.pacto.api.payment.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "결제 상세 응답")
public class PaymentDetailResponse {

    @Schema(description = "결제 ID", example = "1")
    private final Long paymentId;

    @Schema(description = "서버에서 생성한 결제 요청 번호", example = "payment_8d2f6c7a")
    private final String merchantUid;

    @Schema(description = "포트원 결제 고유 번호", example = "imp_1234567890")
    private final String impUid;

    @Schema(description = "결제 금액", example = "10000")
    private final int amount;

    @Schema(description = "결제 상태", example = "PAID")
    private final PaymentStatus status;

    @Schema(description = "결제 요청 생성 시각", example = "2026-06-23T13:30:00")
    private final LocalDateTime createdAt;

    @Schema(description = "결제 상태 최종 변경 시각", example = "2026-06-23T13:31:00")
    private final LocalDateTime updatedAt;

    private PaymentDetailResponse(
            Long paymentId,
            String merchantUid,
            String impUid,
            int amount,
            PaymentStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.paymentId = paymentId;
        this.merchantUid = merchantUid;
        this.impUid = impUid;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentDetailResponse from(Payment payment) {
        return new PaymentDetailResponse(
                payment.getPaymentId(),
                payment.getMerchantUid(),
                payment.getImpUid(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
