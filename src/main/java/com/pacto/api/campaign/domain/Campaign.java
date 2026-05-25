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
    private String guidelines;

    @Column(nullable = false)
    private LocalDateTime deadline;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}