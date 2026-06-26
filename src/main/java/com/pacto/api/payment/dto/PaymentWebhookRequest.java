package com.pacto.api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포트원 V2 결제 웹훅 요청")
public record PaymentWebhookRequest(
        @Schema(description = "웹훅 이벤트 타입", example = "Transaction.Paid")
        String type,

        @Schema(description = "웹훅 이벤트 데이터")
        WebhookData data
) {

    public boolean isPaidTransaction() {
        return "Transaction.Paid".equals(type);
    }

    public String paymentId() {
        return data == null ? null : data.paymentId();
    }

    public record WebhookData(
            @Schema(description = "고객사 결제 건 ID", example = "payment_8d2f6c7a")
            String paymentId,

            @Schema(description = "포트원 상점 ID", example = "store-ae356798-3d20-4969-b739-14c6b0e1a667")
            String storeId,

            @Schema(description = "포트원 결제 시도 ID", example = "55451513-9763-4a7a-bb43-78a4c65be843")
            String transactionId
    ) {
    }
}
