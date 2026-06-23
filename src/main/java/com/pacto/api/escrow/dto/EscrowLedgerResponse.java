package com.pacto.api.escrow.dto;

import com.pacto.api.auth.entity.User;
import com.pacto.api.campaign.domain.Campaign;
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

    @Schema(description = "캠페인 제목", example = "여름 캠페인")
    private final String campaignTitle;

    @Schema(description = "블로거 ID", example = "42")
    private final Long bloggerId;

    @Schema(description = "블로거 표시 이름", example = "blogger@example.com")
    private final String bloggerName;

    @Schema(description = "블로거 이메일", example = "blogger@example.com")
    private final String bloggerEmail;

    @Schema(description = "잠금 금액", example = "50000")
    private final int amount;

    @Schema(description = "에스크로 상태", example = "LOCKED")
    private final EscrowStatus status;

    @Schema(description = "생성 시각", example = "2026-05-26T10:00:00")
    private final LocalDateTime createdAt;

    private EscrowLedgerResponse(
            Long escrowId,
            Long campaignId,
            String campaignTitle,
            Long bloggerId,
            String bloggerName,
            String bloggerEmail,
            int amount,
            EscrowStatus status,
            LocalDateTime createdAt
    ) {
        this.escrowId = escrowId;
        this.campaignId = campaignId;
        this.campaignTitle = campaignTitle;
        this.bloggerId = bloggerId;
        this.bloggerName = bloggerName;
        this.bloggerEmail = bloggerEmail;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static EscrowLedgerResponse from(EscrowLedger escrow) {
        return new EscrowLedgerResponse(
                escrow.getEscrowId(), escrow.getCampaignId(),
                null, escrow.getBloggerId(), null, null,
                escrow.getAmount(), escrow.getStatus(), escrow.getCreatedAt()
        );
    }

    public static EscrowLedgerResponse from(EscrowLedger escrow, Campaign campaign, User blogger) {
        return new EscrowLedgerResponse(
                escrow.getEscrowId(),
                escrow.getCampaignId(),
                campaign.getTitle(),
                escrow.getBloggerId(),
                blogger.getEmail(),
                blogger.getEmail(),
                escrow.getAmount(),
                escrow.getStatus(),
                escrow.getCreatedAt()
        );
    }
}
