package com.pacto.api.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "출금 신청 요청")
public class WithdrawRequest {

    @Schema(description = "출금 금액", example = "30000")
    private int amount;

    @Schema(description = "은행명", example = "카카오뱅크")
    private String bankName;

    @Schema(description = "계좌번호", example = "123-456-789012")
    private String accountNumber;
}
