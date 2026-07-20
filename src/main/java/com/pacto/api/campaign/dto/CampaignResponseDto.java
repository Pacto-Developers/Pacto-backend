package com.pacto.api.campaign.dto;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Schema(description = "캠페인 응답")
public class CampaignResponseDto {

    @Schema(description = "캠페인 ID", example = "1")
    private final Long campaignId;

    @Schema(description = "광고주 ID", example = "10")
    private final Long advertiserId;

    @Schema(description = "캠페인 제목", example = "여름 캠페인")
    private final String title;

    @Schema(description = "썸네일 Presigned URL")
    private final String thumbnailUrl;

    @Schema(description = "리워드 포인트", example = "50000")
    private final Integer rewardPoint;

    @Schema(description = "캠페인 상태", example = "RECRUITING")
    private final CampaignStatus status;

    @Schema(description = "가이드라인")
    private final Map<String, Object> guidelines;

    @Schema(description = "가이드라인 이미지 Presigned URL 목록")
    private final List<String> guidelineImageUrls;

    @Schema(description = "모집 마감일시")
    private final LocalDateTime deadline;

    @Schema(description = "총 모집 인원", example = "10")
    private final Integer totalSlots;

    @Schema(description = "잔여 모집 인원", example = "7")
    private final Integer remainingSlots;

    @Schema(description = "생성 시각")
    private final LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private final LocalDateTime updatedAt;

    private CampaignResponseDto(
            Long campaignId,
            Long advertiserId,
            String title,
            String thumbnailUrl,
            Integer rewardPoint,
            CampaignStatus status,
            Map<String, Object> guidelines,
            List<String> guidelineImageUrls,
            LocalDateTime deadline,
            Integer totalSlots,
            Integer remainingSlots,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.campaignId = campaignId;
        this.advertiserId = advertiserId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.rewardPoint = rewardPoint;
        this.status = status;
        this.guidelines = guidelines;
        this.guidelineImageUrls = guidelineImageUrls;
        this.deadline = deadline;
        this.totalSlots = totalSlots;
        this.remainingSlots = remainingSlots;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CampaignResponseDto from(Campaign campaign, String thumbnailUrl, List<String> guidelineImageUrls) {
        return new CampaignResponseDto(
                campaign.getCampaignId(),
                campaign.getAdvertiserId(),
                campaign.getTitle(),
                thumbnailUrl,
                campaign.getRewardPoint(),
                campaign.getStatus(),
                campaign.getGuidelines(),
                guidelineImageUrls,
                campaign.getDeadline(),
                campaign.getTotalSlots(),
                campaign.getRemainingSlots(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}
