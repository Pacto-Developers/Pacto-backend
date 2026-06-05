package com.pacto.api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "결제 검증 요청")
public class PaymentVerifyRequest {

    @Schema(description = "서버에서 생성한 결제 요청 번호", example = "payment_8d2f6c7a")
    private String merchantUid;

    @Schema(description = "포트원 결제 고유 번호", example = "imp_1234567890")
    private String impUid;
}
