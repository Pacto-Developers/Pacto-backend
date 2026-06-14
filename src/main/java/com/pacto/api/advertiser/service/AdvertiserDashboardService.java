package com.pacto.api.advertiser.service;

import com.pacto.api.advertiser.dto.AdvertiserDashboardResponse;
import com.pacto.api.application.domain.ApplicationStatus;
import com.pacto.api.application.dto.ApplicationResponse;
import com.pacto.api.application.repository.ApplicationRepository;
import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.exception.WalletNotFoundException;
import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.entity.EscrowStatus;
import com.pacto.api.escrow.repository.EscrowLedgerRepository;
import com.pacto.api.mission.domain.Mission;
import com.pacto.api.mission.domain.MissionStatus;
import com.pacto.api.mission.repository.MissionRepository;
import com.pacto.api.wallet.dto.PointHistoryResponse;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.repository.PointHistoryRepository;
import com.pacto.api.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final WalletRepository walletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final EscrowLedgerRepository escrowLedgerRepository;

    public AdvertiserDashboardResponse getDashboard(Long advertiserId) {
        Wallet wallet = walletRepository.findByUserId(advertiserId)
                .orElseThrow(WalletNotFoundException::new);
        AdvertiserDashboardResponse.WalletSummary walletSummary =
                new AdvertiserDashboardResponse.WalletSummary(wallet.getBalance(), wallet.getLockedBalance());
        List<PointHistoryResponse> recentPointHistories = getRecentPointHistories(wallet);

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
                    walletSummary,
                    campaignSummary,
                    new AdvertiserDashboardResponse.ApplicationSummary(0, 0),
                    new AdvertiserDashboardResponse.MissionSummary(0, 0, 0),
                    emptyEscrowSummary(),
                    List.of(),
                    List.of(),
                    recentPointHistories
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
        AdvertiserDashboardResponse.EscrowSummary escrowSummary = getEscrowSummary(campaignIds);

        return new AdvertiserDashboardResponse(
                walletSummary,
                campaignSummary,
                new AdvertiserDashboardResponse.ApplicationSummary(pendingApplications, acceptedApplications),
                new AdvertiserDashboardResponse.MissionSummary(submittedMissions, approvedMissions, rejectedMissions),
                escrowSummary,
                recentApplications,
                pendingMissions,
                recentPointHistories
        );
    }

    private List<PointHistoryResponse> getRecentPointHistories(Wallet wallet) {
        return pointHistoryRepository.findByWallet_WalletId(
                        wallet.getWalletId(),
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .getContent()
                .stream()
                .map(PointHistoryResponse::from)
                .toList();
    }

    private AdvertiserDashboardResponse.EscrowSummary getEscrowSummary(List<Long> campaignIds) {
        List<EscrowLedger> escrows = escrowLedgerRepository.findByCampaignIdIn(campaignIds);
        return new AdvertiserDashboardResponse.EscrowSummary(
                countByStatus(escrows, EscrowStatus.LOCKED),
                countByStatus(escrows, EscrowStatus.RELEASED),
                countByStatus(escrows, EscrowStatus.CANCELED),
                sumAmountByStatus(escrows, EscrowStatus.LOCKED),
                sumAmountByStatus(escrows, EscrowStatus.RELEASED),
                sumAmountByStatus(escrows, EscrowStatus.CANCELED)
        );
    }

    private AdvertiserDashboardResponse.EscrowSummary emptyEscrowSummary() {
        return new AdvertiserDashboardResponse.EscrowSummary(0, 0, 0, 0, 0, 0);
    }

    private long countByStatus(List<EscrowLedger> escrows, EscrowStatus status) {
        return escrows.stream()
                .filter(escrow -> escrow.getStatus() == status)
                .count();
    }

    private int sumAmountByStatus(List<EscrowLedger> escrows, EscrowStatus status) {
        return escrows.stream()
                .filter(escrow -> escrow.getStatus() == status)
                .mapToInt(EscrowLedger::getAmount)
                .sum();
    }
}
