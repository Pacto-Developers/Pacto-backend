package com.pacto.api.campaign.service;

import com.pacto.api.application.domain.Application;
import com.pacto.api.application.domain.ApplicationStatus;
import com.pacto.api.application.repository.ApplicationRepository;
import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.dto.CampaignRequestDto;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.exception.CampaignAccessDeniedException;
import com.pacto.api.escrow.service.EscrowLockService;
import com.pacto.api.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
class CampaignServiceTest {

    @Mock CampaignRepository campaignRepository;
    @Mock ApplicationRepository applicationRepository;
    @Mock EscrowLockService escrowLockService;
    @Mock NotificationService notificationService;
    @InjectMocks CampaignService campaignService;

    @Test
    void getCampaigns은_상태_필터가_없으면_마감_취소_캠페인을_제외한다() {
        Pageable pageable = Pageable.ofSize(20);
        Page<Campaign> page = new PageImpl<>(List.of());
        when(campaignRepository.findByStatusNotIn(
                List.of(CampaignStatus.CLOSED, CampaignStatus.CANCELLED), pageable
        )).thenReturn(page);

        Page<Campaign> result = campaignService.getCampaigns(null, pageable);

        assertThat(result).isSameAs(page);
        verify(campaignRepository).findByStatusNotIn(
                List.of(CampaignStatus.CLOSED, CampaignStatus.CANCELLED), pageable
        );
        verify(campaignRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getCampaigns은_상태_필터가_있으면_해당_상태만_조회한다() {
        Pageable pageable = Pageable.ofSize(20);
        Page<Campaign> page = new PageImpl<>(List.of());
        when(campaignRepository.findByStatus(CampaignStatus.CLOSED, pageable)).thenReturn(page);

        Page<Campaign> result = campaignService.getCampaigns(CampaignStatus.CLOSED, pageable);

        assertThat(result).isSameAs(page);
        verify(campaignRepository).findByStatus(CampaignStatus.CLOSED, pageable);
        verify(campaignRepository, never()).findByStatusNotIn(any(), any());
    }

    @Test
    void createCampaign은_캠페인_생성_후_총_예산을_잠근다() {
        CampaignRequestDto request = new CampaignRequestDto();
        ReflectionTestUtils.setField(request, "title", "캠페인");
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
    void closeCampaign은_소유자의_캠페인을_마감한다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

        Campaign result = campaignService.closeCampaign(10L, 1L);

        assertThat(result.getStatus()).isEqualTo(CampaignStatus.CLOSED);
        verify(escrowLockService, never()).refundUnusedBudget(10L);
    }

    @Test
    void proceedCampaign은_미선정_슬롯_예산을_환불한다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        campaign.closeManually();
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(applicationRepository.findByCampaignIdAndStatus(any(), any())).thenReturn(List.of());

        Campaign result = campaignService.proceedCampaign(10L, 1L);

        assertThat(result.getStatus()).isEqualTo(CampaignStatus.IN_PROGRESS);
        verify(escrowLockService).refundUnusedBudget(10L);
    }

    @Test
    void proceedCampaign은_대기_중인_지원자를_거절하고_알림을_생성한다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        campaign.closeManually();
        Application application = new Application(10L, 2L);
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(applicationRepository.findByCampaignIdAndStatus(10L, ApplicationStatus.PENDING))
                .thenReturn(List.of(application));

        campaignService.proceedCampaign(10L, 1L);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        verify(notificationService).notifyApplicationRejected(2L, 10L, "캠페인");
    }

    @Test
    void cancelCampaign은_미선정_슬롯_예산을_환불한다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(applicationRepository.findByCampaignIdAndStatus(any(), any())).thenReturn(List.of());

        Campaign result = campaignService.cancelCampaign(10L, 1L);

        assertThat(result.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
        verify(escrowLockService).refundUnusedBudget(10L);
    }

    @Test
    void closeCampaign은_소유자가_아니면_거부한다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.closeCampaign(10L, 2L))
                .isInstanceOf(CampaignAccessDeniedException.class);

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RECRUITING);
        verifyNoInteractions(escrowLockService, applicationRepository);
    }

    @Test
    void proceedCampaign은_소유자가_아니면_거부한다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        campaign.closeManually();
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.proceedCampaign(10L, 2L))
                .isInstanceOf(CampaignAccessDeniedException.class);

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CLOSED);
        verifyNoInteractions(escrowLockService, applicationRepository);
    }

    @Test
    void cancelCampaign은_소유자가_아니면_거부한다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.cancelCampaign(10L, 2L))
                .isInstanceOf(CampaignAccessDeniedException.class);

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RECRUITING);
        verifyNoInteractions(escrowLockService, applicationRepository);
    }
}
