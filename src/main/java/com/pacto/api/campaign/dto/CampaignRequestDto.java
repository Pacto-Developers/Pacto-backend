package com.pacto.api.campaign.dto;

import com.pacto.api.campaign.domain.CampaignStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@NoArgsConstructor
public class CampaignRequestDto {
    private String title;
    private Integer rewardPoint;
    private Map<String, Object> guidelines;
    private LocalDateTime deadline;
    private Integer totalSlots;
}