package com.pacto.api.wallet.dto;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.wallet.entity.PointHistory;
import com.pacto.api.wallet.entity.PointHistoryReferenceType;
import com.pacto.api.wallet.entity.PointHistoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "포인트 변동 내역 응답")
public class PointHistoryResponse {

    @Schema(description = "내역 ID", example = "1")
    private final Long historyId;

    @Schema(description = "변동 금액", example = "10000")
    private final int amount;

    @Schema(description = "변동 유형", example = "CHARGE")
    private final PointHistoryType type;

    @Schema(description = "연관 ID (캠페인 등, 없을 수 있음)", example = "null")
    private final Long referenceId;

    @Schema(description = "연관 대상 타입", example = "CAMPAIGN")
    private final PointHistoryReferenceType referenceType;

    @Schema(description = "연관 캠페인 ID", example = "10")
    private final Long campaignId;

    @Schema(description = "연관 캠페인 제목", example = "여름 캠페인")
    private final String campaignTitle;

    @Schema(description = "발생 시각", example = "2026-05-26T10:00:00")
    private final LocalDateTime createdAt;

    private PointHistoryResponse(Long historyId, int amount, PointHistoryType type,
                                 Long referenceId, PointHistoryReferenceType referenceType,
                                 Long campaignId, String campaignTitle, LocalDateTime createdAt) {
        this.historyId = historyId;
        this.amount = amount;
        this.type = type;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.campaignId = campaignId;
        this.campaignTitle = campaignTitle;
        this.createdAt = createdAt;
    }

    public static PointHistoryResponse from(PointHistory history) {
        return from(history, null);
    }

    public static PointHistoryResponse from(PointHistory history, Campaign campaign) {
        return new PointHistoryResponse(
                history.getHistoryId(), history.getAmount(),
                history.getType(), history.getReferenceId(),
                resolveReferenceType(history),
                campaign == null ? null : campaign.getCampaignId(),
                campaign == null ? null : campaign.getTitle(),
                history.getCreatedAt()
        );
    }

    private static PointHistoryReferenceType resolveReferenceType(PointHistory history) {
        if (history.getReferenceType() != null) {
            return history.getReferenceType();
        }
        return PointHistoryReferenceType.infer(history.getType());
    }
}
