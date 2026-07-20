package com.pacto.api.mission.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.campaign.service.CampaignService;
import com.pacto.api.escrow.service.EscrowSettlementService;
import com.pacto.api.mission.domain.Mission;
import com.pacto.api.mission.domain.MissionStatus;
import com.pacto.api.mission.repository.MissionRepository;
import com.pacto.api.notification.service.NotificationService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock MissionRepository missionRepository;
    @Mock EscrowSettlementService escrowSettlementService;
    @Mock CampaignRepository campaignRepository;
    @Mock CampaignService campaignService;
    @Mock NotificationService notificationService;
    @InjectMocks MissionService missionService;

    @Test
    void 미션을_승인하면_블로거에게_승인_알림을_생성한다() {
        Campaign campaign = campaign();
        Mission mission = submittedMission();
        when(missionRepository.findById(20L)).thenReturn(Optional.of(mission));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(missionRepository.save(mission)).thenReturn(mission);
        when(missionRepository.findByCampaignId(10L)).thenReturn(List.of(mission));

        Mission result = missionService.approveMission(20L, 1L);

        assertThat(result.getStatus()).isEqualTo(MissionStatus.APPROVED);
        verify(escrowSettlementService).release(30L);
        verify(notificationService).notifyMissionApproved(2L, 20L, "팩토 캠페인");
    }

    @Test
    void 미션을_반려하면_블로거에게_반려_알림을_생성한다() {
        Campaign campaign = campaign();
        Mission mission = submittedMission();
        when(missionRepository.findById(20L)).thenReturn(Optional.of(mission));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(missionRepository.save(mission)).thenReturn(mission);
        when(missionRepository.findByCampaignId(10L)).thenReturn(List.of(mission));

        Mission result = missionService.rejectMission(20L, 1L);

        assertThat(result.getStatus()).isEqualTo(MissionStatus.REJECTED);
        verify(escrowSettlementService).cancel(30L);
        verify(notificationService).notifyMissionRejected(2L, 20L, "팩토 캠페인");
    }

    private Campaign campaign() {
        Campaign campaign = new Campaign(
                1L,
                "팩토 캠페인",
                null,
                10_000,
                Map.of(),
                LocalDateTime.now().plusDays(1),
                1
        );
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        return campaign;
    }

    private Mission submittedMission() {
        Mission mission = new Mission(10L, 2L, 30L);
        ReflectionTestUtils.setField(mission, "missionId", 20L);
        mission.submit("https://example.com/review");
        return mission;
    }
}
