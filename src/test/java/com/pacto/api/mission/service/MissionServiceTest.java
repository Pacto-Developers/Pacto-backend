package com.pacto.api.mission.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.campaign.service.CampaignService;
import com.pacto.api.common.exception.CampaignAccessDeniedException;
import com.pacto.api.escrow.service.EscrowSettlementService;
import com.pacto.api.mission.domain.Mission;
import com.pacto.api.mission.domain.MissionStatus;
import com.pacto.api.mission.repository.MissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock MissionRepository missionRepository;
    @Mock EscrowSettlementService escrowSettlementService;
    @Mock CampaignRepository campaignRepository;
    @Mock CampaignService campaignService;
    @InjectMocks MissionService missionService;

    @Test
    void getMissionsByCampaignId는_캠페인_소유자의_미션만_조회한다() {
        Campaign campaign = new Campaign(
                1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now().plusDays(7), 3
        );
        List<Mission> missions = List.of(new Mission(10L, 2L, 100L));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(missionRepository.findByCampaignId(10L)).thenReturn(missions);

        List<Mission> result = missionService.getMissionsByCampaignId(10L, 1L);

        assertThat(result).isSameAs(missions);
    }

    @Test
    void getMissionsByCampaignId는_캠페인_소유자가_아니면_거부한다() {
        Campaign campaign = new Campaign(
                1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now().plusDays(7), 3
        );
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> missionService.getMissionsByCampaignId(10L, 2L))
                .isInstanceOf(CampaignAccessDeniedException.class);

        verifyNoInteractions(missionRepository);
    }

    @Test
    void approveMission은_마지막_미션이_승인되면_캠페인을_완료_처리한다() {
        Campaign campaign = campaign(1L, 10L);
        Mission target = mission(100L, 10L, MissionStatus.SUBMITTED);
        Mission other = mission(101L, 10L, MissionStatus.APPROVED);
        when(missionRepository.findById(100L)).thenReturn(Optional.of(target));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(missionRepository.findByCampaignId(10L)).thenReturn(List.of(target, other));

        missionService.approveMission(100L, 1L);

        verify(campaignService).completeCampaign(10L);
    }

    @Test
    void approveMission은_미종결_미션이_남아있으면_캠페인을_완료_처리하지_않는다() {
        Campaign campaign = campaign(1L, 10L);
        Mission target = mission(100L, 10L, MissionStatus.SUBMITTED);
        Mission other = mission(101L, 10L, MissionStatus.SUBMITTED);
        when(missionRepository.findById(100L)).thenReturn(Optional.of(target));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(missionRepository.findByCampaignId(10L)).thenReturn(List.of(target, other));

        missionService.approveMission(100L, 1L);

        verify(campaignService, never()).completeCampaign(any());
    }

    @Test
    void rejectMission은_마지막_미션이_반려되면_캠페인을_완료_처리한다() {
        Campaign campaign = campaign(1L, 10L);
        Mission target = mission(100L, 10L, MissionStatus.SUBMITTED);
        Mission other = mission(101L, 10L, MissionStatus.CANCELLED);
        when(missionRepository.findById(100L)).thenReturn(Optional.of(target));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(missionRepository.findByCampaignId(10L)).thenReturn(List.of(target, other));

        missionService.rejectMission(100L, 1L);

        verify(campaignService).completeCampaign(10L);
    }

    @Test
    void rejectMission은_미종결_미션이_남아있으면_캠페인을_완료_처리하지_않는다() {
        Campaign campaign = campaign(1L, 10L);
        Mission target = mission(100L, 10L, MissionStatus.SUBMITTED);
        Mission other = mission(101L, 10L, MissionStatus.SUBMITTED);
        when(missionRepository.findById(100L)).thenReturn(Optional.of(target));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(missionRepository.findByCampaignId(10L)).thenReturn(List.of(target, other));

        missionService.rejectMission(100L, 1L);

        verify(campaignService, never()).completeCampaign(any());
    }

    @Test
    void cancelMission은_마지막_미션이_취소되면_캠페인을_완료_처리한다() {
        Campaign campaign = campaign(1L, 10L);
        Mission target = mission(100L, 10L, MissionStatus.READY);
        Mission approved = mission(101L, 10L, MissionStatus.APPROVED);
        Mission rejected = mission(102L, 10L, MissionStatus.REJECTED);
        when(missionRepository.findById(100L)).thenReturn(Optional.of(target));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(missionRepository.findByCampaignId(10L)).thenReturn(List.of(target, approved, rejected));

        missionService.cancelMission(100L, 1L);

        verify(campaignService).completeCampaign(10L);
    }

    @Test
    void cancelMission은_미종결_미션이_남아있으면_캠페인을_완료_처리하지_않는다() {
        Campaign campaign = campaign(1L, 10L);
        Mission target = mission(100L, 10L, MissionStatus.READY);
        Mission other = mission(101L, 10L, MissionStatus.SUBMITTED);
        when(missionRepository.findById(100L)).thenReturn(Optional.of(target));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(missionRepository.findByCampaignId(10L)).thenReturn(List.of(target, other));

        missionService.cancelMission(100L, 1L);

        verify(campaignService, never()).completeCampaign(any());
    }

    private Campaign campaign(Long advertiserId, Long campaignId) {
        Campaign campaign = new Campaign(
                advertiserId, "캠페인", null, 50000, Map.of(), LocalDateTime.now().plusDays(7), 3
        );
        ReflectionTestUtils.setField(campaign, "campaignId", campaignId);
        return campaign;
    }

    private Mission mission(Long missionId, Long campaignId, MissionStatus status) {
        Mission mission = new Mission(campaignId, 2L, 500L);
        ReflectionTestUtils.setField(mission, "missionId", missionId);
        ReflectionTestUtils.setField(mission, "status", status);
        return mission;
    }
}
