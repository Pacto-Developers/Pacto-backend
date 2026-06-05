package com.pacto.api.application.service;

import com.pacto.api.application.domain.Application;
import com.pacto.api.application.dto.ApplicationResponse;
import com.pacto.api.application.repository.ApplicationRepository;
import com.pacto.api.common.exception.ApplicationNotFoundException;
import com.pacto.api.escrow.service.EscrowLockService;
import com.pacto.api.mission.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final EscrowLockService escrowLockService;
    private final MissionService missionService;

    @Transactional
    public Application apply(Long campaignId, Long bloggerId) {
        Application application = new Application(campaignId, bloggerId);
        return applicationRepository.save(application);
    }

    @Transactional
    public Application acceptApplication(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(ApplicationNotFoundException::new);

        application.accept();
        applicationRepository.save(application);

        Long escrowId = escrowLockService.lock(application.getCampaignId(), application.getBloggerId());
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
    public Application cancelApplication(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(ApplicationNotFoundException::new);
        application.cancel();
        return applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByCampaign(Long campaignId) {
        return applicationRepository.findByCampaignIdWithBloggerEmail(campaignId);
    }

    @Transactional(readOnly = true)
    public List<Application> getMyApplications(Long bloggerId) {
        return applicationRepository.findByBloggerId(bloggerId);
    }
}
