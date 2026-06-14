package com.pacto.api.advertiser.dto;

import com.pacto.api.application.dto.ApplicationResponse;
import com.pacto.api.mission.domain.Mission;
import com.pacto.api.wallet.dto.PointHistoryResponse;

import java.util.List;

public record AdvertiserDashboardResponse(
        WalletSummary wallet,
        CampaignSummary campaignSummary,
        ApplicationSummary applicationSummary,
        MissionSummary missionSummary,
        EscrowSummary escrowSummary,
        List<ApplicationResponse> recentApplications,
        List<Mission> pendingMissions,
        List<PointHistoryResponse> recentPointHistories
) {
    public record WalletSummary(
            int balance,
            int lockedBalance
    ) {}

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

    public record EscrowSummary(
            long lockedEscrows,
            long releasedEscrows,
            long canceledEscrows,
            int lockedAmount,
            int releasedAmount,
            int canceledAmount
    ) {}
}
