package com.pacto.api.application.service;

import com.pacto.api.application.domain.Application;
import com.pacto.api.application.domain.ApplicationStatus;
import com.pacto.api.application.repository.ApplicationRepository;
import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.escrow.service.EscrowLockService;
import com.pacto.api.mission.service.MissionService;
import com.pacto.api.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock CampaignRepository campaignRepository;
    @Mock EscrowLockService escrowLockService;
    @Mock MissionService missionService;
    @Mock NotificationService notificationService;
    @InjectMocks ApplicationService applicationService;

    @Test
    void 지원자를_선정하면_블로거에게_선정_알림을_생성한다() {
        Campaign campaign = closedCampaign();
        Application application = application();
        when(applicationRepository.findById(20L)).thenReturn(Optional.of(application));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(applicationRepository.save(application)).thenReturn(application);
        when(escrowLockService.createEscrowForSelection(10L, 2L)).thenReturn(30L);

        Application result = applicationService.acceptApplication(20L, 1L);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        verify(notificationService).notifyApplicationAccepted(2L, 10L, "팩토 캠페인");
        verify(missionService).acceptMission(10L, 2L, 30L, 20L);
    }

    @Test
    void 지원자를_거절하면_블로거에게_미선정_알림을_생성한다() {
        Campaign campaign = closedCampaign();
        Application application = application();
        when(applicationRepository.findById(20L)).thenReturn(Optional.of(application));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(applicationRepository.save(application)).thenReturn(application);

        Application result = applicationService.rejectApplication(20L, 1L);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        verify(notificationService).notifyApplicationRejected(2L, 10L, "팩토 캠페인");
    }

    private Campaign closedCampaign() {
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
        campaign.close();
        return campaign;
    }

    private Application application() {
        Application application = new Application(10L, 2L);
        ReflectionTestUtils.setField(application, "applicationId", 20L);
        return application;
    }
}
