package com.pacto.api.advertiser.service;

import com.pacto.api.advertiser.dto.AdvertiserDashboardResponse;
import com.pacto.api.application.domain.ApplicationStatus;
import com.pacto.api.application.dto.ApplicationResponse;
import com.pacto.api.application.repository.ApplicationRepository;
import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.mission.domain.Mission;
import com.pacto.api.mission.domain.MissionStatus;
import com.pacto.api.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdvertiserDashboardService {

    private final CampaignRepository campaignRepository;
    private final ApplicationRepository applicationRepository;
    private final MissionRepository missionRepository;

    public AdvertiserDashboardResponse getDashboard(Long advertiserId) {
        List<Campaign> campaigns = campaignRepository.findByAdvertiserId(advertiserId);
        List<Long> campaignIds = campaigns.stream().map(Campaign::getCampaignId).toList();

        long total = campaigns.size();
        long recruiting = campaignRepository.countByAdvertiserIdAndStatus(advertiserId, CampaignStatus.RECRUITING);
        long inProgress = campaignRepository.countByAdvertiserIdAndStatus(advertiserId, CampaignStatus.IN_PROGRESS);
        long completed = campaignRepository.countByAdvertiserIdAndStatus(advertiserId, CampaignStatus.COMPLETED);

        AdvertiserDashboardResponse.CampaignSummary campaignSummary =
                new AdvertiserDashboardResponse.CampaignSummary(total, recruiting, inProgress, completed);

        if (campaignIds.isEmpty()) {
            return new AdvertiserDashboardResponse(
                    campaignSummary,
                    new AdvertiserDashboardResponse.ApplicationSummary(0, 0),
                    new AdvertiserDashboardResponse.MissionSummary(0, 0, 0),
                    List.of(),
                    List.of()
            );
        }

        long pendingApplications = applicationRepository.countByCampaignIdInAndStatus(campaignIds, ApplicationStatus.PENDING);
        long acceptedApplications = applicationRepository.countByCampaignIdInAndStatus(campaignIds, ApplicationStatus.ACCEPTED);
        List<ApplicationResponse> recentApplications = applicationRepository
                .findRecentByCampaignIdIn(campaignIds, PageRequest.of(0, 5));

        long submittedMissions = missionRepository.countByCampaignIdInAndStatus(campaignIds, MissionStatus.SUBMITTED);
        long approvedMissions = missionRepository.countByCampaignIdInAndStatus(campaignIds, MissionStatus.APPROVED);
        long rejectedMissions = missionRepository.countByCampaignIdInAndStatus(campaignIds, MissionStatus.REJECTED);
        List<Mission> pendingMissions = missionRepository.findByCampaignIdInAndStatus(campaignIds, MissionStatus.SUBMITTED);

        return new AdvertiserDashboardResponse(
                campaignSummary,
                new AdvertiserDashboardResponse.ApplicationSummary(pendingApplications, acceptedApplications),
                new AdvertiserDashboardResponse.MissionSummary(submittedMissions, approvedMissions, rejectedMissions),
                recentApplications,
                pendingMissions
        );
    }
}
