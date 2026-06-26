package com.pacto.api.mission.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.campaign.service.CampaignService;
import com.pacto.api.common.exception.CampaignAccessDeniedException;
import com.pacto.api.common.exception.CampaignNotFoundException;
import com.pacto.api.common.exception.InvalidCampaignStatusException;
import com.pacto.api.common.exception.MissionAccessDeniedException;
import com.pacto.api.common.exception.MissionNotFoundException;
import com.pacto.api.escrow.service.EscrowSettlementService;
import com.pacto.api.mission.domain.Mission;
import com.pacto.api.mission.domain.MissionStatus;
import com.pacto.api.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final EscrowSettlementService escrowSettlementService;
    private final CampaignRepository campaignRepository;
    private final CampaignService campaignService;

    private static final Set<MissionStatus> TERMINAL_STATUSES =
            EnumSet.of(MissionStatus.APPROVED, MissionStatus.REJECTED, MissionStatus.CANCELLED);

    // 내 미션 목록 조회
    @Transactional(readOnly = true)
    public List<Mission> getMyMissions(Long bloggerId, MissionStatus status) {
        if (status != null) {
            return missionRepository.findByBloggerIdAndStatus(bloggerId, status);
        }
        return missionRepository.findByBloggerId(bloggerId);
    }

    // URL 제출
    @Transactional
    public Mission submitMission(Long missionId, Long bloggerId, String submittedUrl) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(MissionNotFoundException::new);
        if (!mission.getBloggerId().equals(bloggerId)) {
            throw new MissionAccessDeniedException();
        }
        Campaign campaign = campaignRepository.findById(mission.getCampaignId())
                .orElseThrow(CampaignNotFoundException::new);
        if (campaign.getStatus() != CampaignStatus.IN_PROGRESS) {
            throw new InvalidCampaignStatusException("캠페인이 진행 중일 때만 미션을 제출할 수 있습니다.");
        }
        mission.submit(submittedUrl);
        return missionRepository.save(mission);
    }

    // 미션 승인
    @Transactional
    public Mission approveMission(Long missionId, Long advertiserId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionNotFoundException());
        Campaign campaign = campaignRepository.findById(mission.getCampaignId())
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        mission.approve();
        escrowSettlementService.release(mission.getEscrowId());
        missionRepository.save(mission);
        tryCompleteCampaign(mission.getCampaignId());
        return mission;
    }

    // 미션 반려
    @Transactional
    public Mission rejectMission(Long missionId, Long advertiserId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(MissionNotFoundException::new);
        Campaign campaign = campaignRepository.findById(mission.getCampaignId())
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        mission.reject();
        escrowSettlementService.cancel(mission.getEscrowId());
        missionRepository.save(mission);
        tryCompleteCampaign(mission.getCampaignId());
        return mission;
    }

    // 미션 취소
    @Transactional
    public Mission cancelMission(Long missionId, Long advertiserId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(MissionNotFoundException::new);
        Campaign campaign = campaignRepository.findById(mission.getCampaignId())
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        mission.cancel();
        escrowSettlementService.cancel(mission.getEscrowId());
        return missionRepository.save(mission);
    }

    // 미션 수락 (에스크로 LOCK은 ApplicationService에서 처리)
    @Transactional
    public Mission acceptMission(Long campaignId, Long bloggerId, Long escrowId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("캠페인을 찾을 수 없습니다."));
        campaign.decreaseSlot();
        campaignRepository.save(campaign);

        Mission mission = new Mission(campaignId, bloggerId, escrowId);
        return missionRepository.save(mission);
    }

    // 캠페인별 미션 목록 조회
    @Transactional(readOnly = true)
    public List<Mission> getMissionsByCampaignId(Long campaignId) {
        return missionRepository.findByCampaignId(campaignId);
    }

    private void tryCompleteCampaign(Long campaignId) {
        List<Mission> missions = missionRepository.findByCampaignId(campaignId);
        boolean allSettled = !missions.isEmpty() &&
                missions.stream().allMatch(m -> TERMINAL_STATUSES.contains(m.getStatus()));
        if (allSettled) {
            campaignService.completeCampaign(campaignId);
        }
    }
}