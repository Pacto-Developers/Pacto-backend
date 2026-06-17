package com.pacto.api.campaign.domain;

import com.pacto.api.common.exception.CampaignSlotFullException;
import com.pacto.api.common.exception.InvalidCampaignStatusException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "campaigns")
@Getter
@NoArgsConstructor
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long campaignId;

    @Column(nullable = false)
    private Long advertiserId;

    @Column(nullable = false)
    private String title;

    private String thumbnailUrl;

    @Column(nullable = false)
    private Integer rewardPoint;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CampaignStatus status;

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private Map<String, Object> guidelines;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(nullable = false)
    private Integer totalSlots;

    @Column(nullable = false)
    private Integer remainingSlots;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Campaign(Long advertiserId, String title, String thumbnailUrl,
                    Integer rewardPoint, Map<String, Object> guidelines, LocalDateTime deadline,
                    Integer totalSlots) {
        this.advertiserId = advertiserId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.rewardPoint = rewardPoint;
        this.guidelines = guidelines;
        this.deadline = deadline;
        this.status = CampaignStatus.RECRUITING;
        this.totalSlots = totalSlots;
        this.remainingSlots = totalSlots;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 상태 변경
    public void updateStatus(CampaignStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        if (this.status != CampaignStatus.RECRUITING) {
            throw new InvalidCampaignStatusException("모집 중인 캠페인만 진행 중으로 전환할 수 있습니다.");
        }
        this.status = CampaignStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        if (this.status != CampaignStatus.IN_PROGRESS) {
            throw new InvalidCampaignStatusException("진행 중인 캠페인만 완료 처리할 수 있습니다.");
        }
        this.status = CampaignStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == CampaignStatus.CANCELLED) {
            throw new InvalidCampaignStatusException("이미 취소된 캠페인입니다.");
        }
        if (this.status != CampaignStatus.RECRUITING && this.status != CampaignStatus.IN_PROGRESS) {
            throw new InvalidCampaignStatusException("모집 중 또는 진행 중인 캠페인만 취소할 수 있습니다.");
        }
        this.status = CampaignStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    // 슬롯 차감
    public void decreaseSlot() {
        if (this.remainingSlots <= 0) {
            throw new CampaignSlotFullException();
        }
        this.remainingSlots--;
        this.updatedAt = LocalDateTime.now();
    }
}