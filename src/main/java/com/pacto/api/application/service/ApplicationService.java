package com.pacto.api.application.service;

import com.pacto.api.application.domain.Application;
import com.pacto.api.application.domain.ApplicationStatus;
import com.pacto.api.application.dto.ApplicationResponse;
import com.pacto.api.application.repository.ApplicationRepository;
import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.exception.ApplicationAccessDeniedException;
import com.pacto.api.common.exception.ApplicationNotFoundException;
import com.pacto.api.common.exception.CampaignAccessDeniedException;
import com.pacto.api.common.exception.CampaignNotFoundException;
import com.pacto.api.common.exception.CampaignNotOpenException;
import com.pacto.api.common.exception.DuplicateApplicationException;
import com.pacto.api.escrow.service.EscrowLockService;
import com.pacto.api.mission.service.MissionService;
import com.pacto.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CampaignRepository campaignRepository;
    private final EscrowLockService escrowLockService;
    private final MissionService missionService;
    private final NotificationService notificationService;

    @Transactional
    public Application apply(Long campaignId, Long bloggerId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        if (campaign.getStatus() != CampaignStatus.RECRUITING) {
            throw new CampaignNotOpenException();
        }
        if (applicationRepository.existsByCampaignIdAndBloggerIdAndStatusIn(
                campaignId, bloggerId, Arrays.asList(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED))) {
            throw new DuplicateApplicationException();
        }
        Application application = new Application(campaignId, bloggerId);
        return applicationRepository.save(application);
    }

    @Transactional
    public Application acceptApplication(Long applicationId, Long advertiserId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(ApplicationNotFoundException::new);

        Campaign campaign = campaignRepository.findById(application.getCampaignId())
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        if (campaign.getStatus() != CampaignStatus.CLOSED) {
            throw new CampaignNotOpenException("모집 마감된 캠페인에서만 선정할 수 있습니다.");
        }

        application.accept();
        applicationRepository.save(application);

        Long escrowId = escrowLockService.createEscrowForSelection(
                application.getCampaignId(),
                application.getBloggerId()
        );
        missionService.acceptMission(application.getCampaignId(), application.getBloggerId(), escrowId);
        notificationService.notifyApplicationAccepted(
                application.getBloggerId(),
                campaign.getCampaignId(),
                campaign.getTitle()
        );

        return application;
    }

    @Transactional
    public Application rejectApplication(Long applicationId, Long advertiserId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(ApplicationNotFoundException::new);
        Campaign campaign = campaignRepository.findById(application.getCampaignId())
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        application.reject();
        Application savedApplication = applicationRepository.save(application);
        notificationService.notifyApplicationRejected(
                application.getBloggerId(),
                campaign.getCampaignId(),
                campaign.getTitle()
        );
        return savedApplication;
    }

    @Transactional
    public Application cancelApplication(Long applicationId, Long bloggerId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(ApplicationNotFoundException::new);
        if (!application.getBloggerId().equals(bloggerId)) {
            throw new ApplicationAccessDeniedException();
        }
        application.cancel();
        return applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByCampaign(Long campaignId, Long advertiserId, ApplicationStatus status) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        if (status != null) {
            return applicationRepository.findByCampaignIdAndStatusWithBloggerEmail(campaignId, status);
        }
        return applicationRepository.findByCampaignIdWithBloggerEmail(campaignId);
    }

    @Transactional(readOnly = true)
    public List<Application> getMyApplications(Long bloggerId, ApplicationStatus status) {
        if (status != null) {
            return applicationRepository.findByBloggerIdAndStatus(bloggerId, status);
        }
        return applicationRepository.findByBloggerId(bloggerId);
    }
}
