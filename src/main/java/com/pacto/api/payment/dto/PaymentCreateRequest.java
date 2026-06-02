package com.pacto.api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "결제 요청 생성 요청")
public class PaymentCreateRequest {

    @Schema(description = "결제 요청 금액", example = "10000")
    private int amount;
}
