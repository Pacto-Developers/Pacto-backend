package com.pacto.api.mission.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.campaign.service.CampaignService;
import com.pacto.api.common.exception.CampaignAccessDeniedException;
import com.pacto.api.escrow.service.EscrowSettlementService;
import com.pacto.api.mission.domain.Mission;
import com.pacto.api.mission.repository.MissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
