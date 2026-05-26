package com.pacto.api.wallet.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WithdrawRequest {
    private int amount;
    private String bankName;
    private String accountNumber;
}
