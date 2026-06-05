package com.pacto.api.mission.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.exception.MissionNotFoundException;
import com.pacto.api.escrow.service.EscrowLockService;
import com.pacto.api.escrow.service.EscrowSettlementService;
import com.pacto.api.mission.domain.Mission;
import com.pacto.api.mission.domain.MissionStatus;
import com.pacto.api.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final EscrowSettlementService escrowSettlementService;
    private final EscrowLockService escrowLockService;
    private final CampaignRepository campaignRepository;

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
    public Mission submitMission(Long missionId, String submittedUrl) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionNotFoundException());
        mission.submit(submittedUrl);
        return missionRepository.save(mission);
    }

    // 미션 승인
    @Transactional
    public Mission approveMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionNotFoundException());
        mission.approve();
        escrowSettlementService.release(mission.getEscrowId());
        return missionRepository.save(mission);
    }

    // 미션 취소
    @Transactional
    public Mission cancelMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionNotFoundException());
        mission.cancel();
        escrowSettlementService.cancel(mission.getEscrowId());
        return missionRepository.save(mission);
    }

    // 미션 수락
    @Transactional
    public Mission acceptMission(Long campaignId, Long bloggerId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("캠페인을 찾을 수 없습니다."));
        campaign.decreaseSlot();
        campaignRepository.save(campaign);

        Long escrowId = escrowLockService.lock(campaignId, bloggerId);
        Mission mission = new Mission(campaignId, bloggerId, escrowId);
        return missionRepository.save(mission);
    }

    // 캠페인별 미션 목록 조회
    @Transactional(readOnly = true)
    public List<Mission> getMissionsByCampaignId(Long campaignId) {
        return missionRepository.findByCampaignId(campaignId);
    }
}