package com.pacto.api.campaign.service;

import com.pacto.api.application.repository.ApplicationRepository;
import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.dto.CampaignRequestDto;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.escrow.service.EscrowLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock CampaignRepository campaignRepository;
    @Mock ApplicationRepository applicationRepository;
    @Mock EscrowLockService escrowLockService;
    @InjectMocks CampaignService campaignService;

    @Test
    void createCampaign은_캠페인_생성_후_총_예산을_잠근다() {
        CampaignRequestDto request = new CampaignRequestDto();
        ReflectionTestUtils.setField(request, "title", "캠페인");
        ReflectionTestUtils.setField(request, "thumbnailUrl", null);
        ReflectionTestUtils.setField(request, "rewardPoint", 50000);
        ReflectionTestUtils.setField(request, "guidelines", Map.of());
        ReflectionTestUtils.setField(request, "deadline", LocalDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(request, "totalSlots", 3);
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> {
            Campaign campaign = invocation.getArgument(0);
            ReflectionTestUtils.setField(campaign, "campaignId", 10L);
            return campaign;
        });

        Campaign campaign = campaignService.createCampaign(request, 1L);

        assertThat(campaign.getCampaignId()).isEqualTo(10L);
        ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
        verify(escrowLockService).lockCampaignBudget(campaignCaptor.capture());
        assertThat(campaignCaptor.getValue()).isSameAs(campaign);
    }

    @Test
    void closeCampaign은_미선정_슬롯_예산을_환불한다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

        campaignService.closeCampaign(10L);

        verify(escrowLockService).refundUnusedBudget(10L);
    }

    @Test
    void cancelCampaign은_미선정_슬롯_예산을_환불한다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

        campaignService.cancelCampaign(10L);

        verify(escrowLockService).refundUnusedBudget(10L);
    }
}
