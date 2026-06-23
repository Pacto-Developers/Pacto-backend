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
import com.pacto.api.common.exception.CampaignNotFoundException;
import com.pacto.api.common.exception.CampaignNotOpenException;
import com.pacto.api.common.exception.DuplicateApplicationException;
import com.pacto.api.escrow.service.EscrowLockService;
import com.pacto.api.mission.service.MissionService;
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
    public Application acceptApplication(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(ApplicationNotFoundException::new);

        application.accept();
        applicationRepository.save(application);

        Long escrowId = escrowLockService.createEscrowForSelection(
                application.getCampaignId(),
                application.getBloggerId()
        );
        missionService.acceptMission(application.getCampaignId(), application.getBloggerId(), escrowId);

        return application;
    }

    @Transactional
    public Application rejectApplication(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(ApplicationNotFoundException::new);
        application.reject();
        return applicationRepository.save(application);
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
    public List<ApplicationResponse> getApplicationsByCampaign(Long campaignId, ApplicationStatus status) {
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
