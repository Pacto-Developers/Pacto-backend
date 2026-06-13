package com.pacto.api.advertiser.dto;

import com.pacto.api.application.dto.ApplicationResponse;
import com.pacto.api.mission.domain.Mission;

import java.util.List;

public record AdvertiserDashboardResponse(
        CampaignSummary campaignSummary,
        ApplicationSummary applicationSummary,
        MissionSummary missionSummary,
        List<ApplicationResponse> recentApplications,
        List<Mission> pendingMissions
) {
    public record CampaignSummary(
            long totalCampaigns,
            long recruitingCampaigns,
            long inProgressCampaigns,
            long completedCampaigns
    ) {}

    public record ApplicationSummary(
            long pendingApplications,
            long acceptedApplications
    ) {}

    public record MissionSummary(
            long submittedMissions,
            long approvedMissions,
            long rejectedMissions
    ) {}
}
