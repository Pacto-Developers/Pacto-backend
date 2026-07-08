package com.pacto.api.campaign.service;

import com.pacto.api.application.domain.Application;
import com.pacto.api.application.domain.ApplicationStatus;
import com.pacto.api.application.repository.ApplicationRepository;
import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.dto.CampaignRequestDto;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.exception.CampaignAccessDeniedException;
import com.pacto.api.common.exception.CampaignNotFoundException;
import com.pacto.api.common.exception.InvalidCampaignStatusException;
import com.pacto.api.escrow.service.EscrowLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private static final List<CampaignStatus> HIDDEN_FROM_LIST = List.of(
            CampaignStatus.CLOSED, CampaignStatus.CANCELLED
    );

    private final CampaignRepository campaignRepository;
    private final ApplicationRepository applicationRepository;
    private final EscrowLockService escrowLockService;

    // 캠페인 목록 조회 (필터 없으면 취소/마감 캠페인 제외)
    @Transactional(readOnly = true)
    public Page<Campaign> getCampaigns(CampaignStatus status, Pageable pageable) {
        if (status != null) {
            return campaignRepository.findByStatus(status, pageable);
        }
        return campaignRepository.findByStatusNotIn(HIDDEN_FROM_LIST, pageable);
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
        Campaign savedCampaign = campaignRepository.save(campaign);
        escrowLockService.lockCampaignBudget(savedCampaign);
        return savedCampaign;
    }

    @Transactional
    public Campaign closeCampaign(Long campaignId, Long advertiserId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        campaign.closeManually();
        return campaign;
    }

    @Transactional
    public Campaign proceedCampaign(Long campaignId, Long advertiserId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        campaign.proceed();
        escrowLockService.refundUnusedBudget(campaignId);
        rejectPendingApplications(campaignId);
        return campaign;
    }

    @Transactional
    public Campaign completeCampaign(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        campaign.complete();
        return campaign;
    }

    @Transactional
    public Campaign cancelCampaign(Long campaignId, Long advertiserId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        boolean hasAccepted = !applicationRepository.findByCampaignIdAndStatus(campaignId, ApplicationStatus.ACCEPTED).isEmpty();
        if (hasAccepted) {
            throw new InvalidCampaignStatusException("선정된 블로거가 있어 취소할 수 없습니다.");
        }
        campaign.cancel();
        escrowLockService.refundUnusedBudget(campaignId);
        rejectPendingApplications(campaignId);
        return campaign;
    }

    private void rejectPendingApplications(Long campaignId) {
        List<Application> pending = applicationRepository.findByCampaignIdAndStatus(campaignId, ApplicationStatus.PENDING);
        pending.forEach(Application::reject);
    }
}
