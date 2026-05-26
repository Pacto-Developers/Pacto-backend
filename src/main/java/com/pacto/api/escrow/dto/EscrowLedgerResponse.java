package com.pacto.api.escrow.dto;

import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.entity.EscrowStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EscrowLedgerResponse {
    private final Long escrowId;
    private final Long campaignId;
    private final int amount;
    private final EscrowStatus status;
    private final LocalDateTime createdAt;

    private EscrowLedgerResponse(Long escrowId, Long campaignId, int amount,
                                  EscrowStatus status, LocalDateTime createdAt) {
        this.escrowId = escrowId;
        this.campaignId = campaignId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static EscrowLedgerResponse from(EscrowLedger escrow) {
        return new EscrowLedgerResponse(
                escrow.getEscrowId(), escrow.getCampaignId(),
                escrow.getAmount(), escrow.getStatus(), escrow.getCreatedAt()
        );
    }
}
