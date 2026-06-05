package com.pacto.api.campaign.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.dto.CampaignRequestDto;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.exception.CampaignNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;

    // 캠페인 목록 조회
    @Transactional(readOnly = true)
    public Page<Campaign> getCampaigns(CampaignStatus status, Pageable pageable) {
        if (status != null) {
            return campaignRepository.findByStatus(status, pageable);
        }
        return campaignRepository.findAll(pageable);
    }

    // 캠페인 상세 조회
    @Transactional(readOnly = true)
    public Campaign getCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException());
    }

    // 캠페인 등록
    @Transactional
    public Campaign createCampaign(CampaignRequestDto dto, Long advertiserId) {
        Campaign campaign = new Campaign(
                advertiserId,
                dto.getTitle(),
                dto.getThumbnailUrl(),
                dto.getRewardPoint(),
                dto.getGuidelines(),
                dto.getDeadline(),
                dto.getTotalSlots()
        );
        return campaignRepository.save(campaign);
    }

    // 캠페인 상태 변경
    @Transactional
    public Campaign updateCampaignStatus(Long campaignId, CampaignStatus status) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException());
        campaign.updateStatus(status);
        return campaignRepository.save(campaign);
    }
}