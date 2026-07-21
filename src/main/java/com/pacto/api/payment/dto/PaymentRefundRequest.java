package com.pacto.api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "결제 환불 요청")
public class PaymentRefundRequest {

    @Schema(description = "환불 요청 금액", example = "5000")
    private int amount;

    @Schema(description = "환불 사유", example = "광고주 요청")
    private String reason;

    @Schema(
            description = "중복 환불 방지를 위한 고유 키",
            example = "refund_550e8400_e29b_41d4_a716_446655440000"
    )
    private String idempotencyKey;
}
