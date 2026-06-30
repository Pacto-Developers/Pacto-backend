package com.pacto.api.campaign.scheduler;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.repository.CampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignSchedulerTest {

    @Mock CampaignRepository campaignRepository;
    @InjectMocks CampaignScheduler campaignScheduler;

    @Test
    void closeExpiredCampaigns는_만료된_모집중_캠페인을_잔여_슬롯_변경_없이_마감한다() {
        Campaign campaign = new Campaign(
                1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now().minusDays(1), 3);
        campaign.decreaseSlot();
        when(campaignRepository.findByDeadlineBeforeAndStatus(any(LocalDateTime.class), eq(CampaignStatus.RECRUITING)))
                .thenReturn(List.of(campaign));

        campaignScheduler.closeExpiredCampaigns();

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CLOSED);
        assertThat(campaign.getRemainingSlots()).isEqualTo(2);
        verify(campaignRepository).findByDeadlineBeforeAndStatus(
                any(LocalDateTime.class), eq(CampaignStatus.RECRUITING));
        verifyNoMoreInteractions(campaignRepository);
    }

    @Test
    void closeExpiredCampaigns는_만료된_캠페인이_없으면_조회만_하고_종료한다() {
        when(campaignRepository.findByDeadlineBeforeAndStatus(any(LocalDateTime.class), eq(CampaignStatus.RECRUITING)))
                .thenReturn(List.of());

        campaignScheduler.closeExpiredCampaigns();

        verify(campaignRepository).findByDeadlineBeforeAndStatus(
                any(LocalDateTime.class), eq(CampaignStatus.RECRUITING));
        verifyNoMoreInteractions(campaignRepository);
    }
}
