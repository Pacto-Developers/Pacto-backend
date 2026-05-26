package com.pacto.api.campaign.dto;

import com.pacto.api.campaign.domain.CampaignStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CampaignRequestDto {

    private Long advertiserId;
    private String title;
    private String thumbnailUrl;
    private Integer rewardPoint;
    private String guidelines;
    private LocalDateTime deadline;
}