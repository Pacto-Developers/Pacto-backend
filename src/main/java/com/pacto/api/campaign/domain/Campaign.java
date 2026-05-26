package com.pacto.api.campaign.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private String guidelines;

    @Column(nullable = false)
    private LocalDateTime deadline;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Campaign(Long advertiserId, String title, String thumbnailUrl,
                    Integer rewardPoint, String guidelines, LocalDateTime deadline) {
        this.advertiserId = advertiserId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.rewardPoint = rewardPoint;
        this.guidelines = guidelines;
        this.deadline = deadline;
        this.status = CampaignStatus.RECRUITING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 상태 변경
    public void updateStatus(CampaignStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}