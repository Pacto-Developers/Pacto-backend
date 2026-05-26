package com.pacto.api.escrow.dto;

import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.entity.EscrowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "에스크로 잠금 내역 응답")
public class EscrowLedgerResponse {

    @Schema(description = "에스크로 ID", example = "1")
    private final Long escrowId;

    @Schema(description = "캠페인 ID", example = "10")
    private final Long campaignId;

    @Schema(description = "잠금 금액", example = "50000")
    private final int amount;

    @Schema(description = "에스크로 상태", example = "LOCKED")
    private final EscrowStatus status;

    @Schema(description = "생성 시각", example = "2026-05-26T10:00:00")
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
