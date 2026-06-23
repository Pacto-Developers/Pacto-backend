package com.pacto.api.advertiser.service;

import com.pacto.api.advertiser.dto.AdvertiserDashboardResponse;
import com.pacto.api.application.domain.ApplicationStatus;
import com.pacto.api.application.repository.ApplicationRepository;
import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.repository.EscrowLedgerRepository;
import com.pacto.api.mission.domain.MissionStatus;
import com.pacto.api.mission.repository.MissionRepository;
import com.pacto.api.wallet.entity.PointHistory;
import com.pacto.api.wallet.entity.PointHistoryReferenceType;
import com.pacto.api.wallet.entity.PointHistoryType;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.repository.PointHistoryRepository;
import com.pacto.api.wallet.repository.WalletRepository;
import com.pacto.api.wallet.service.PointHistoryResponseMapper;
import com.pacto.api.wallet.dto.PointHistoryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvertiserDashboardServiceTest {

    @Mock CampaignRepository campaignRepository;
    @Mock ApplicationRepository applicationRepository;
    @Mock MissionRepository missionRepository;
    @Mock WalletRepository walletRepository;
    @Mock PointHistoryRepository pointHistoryRepository;
    @Mock EscrowLedgerRepository escrowLedgerRepository;
    @Mock PointHistoryResponseMapper pointHistoryResponseMapper;
    @InjectMocks AdvertiserDashboardService advertiserDashboardService;

    @Test
    void 대시보드에_지갑_에스크로_최근포인트_요약을_포함한다() {
        Campaign campaign = new Campaign(
                1L,
                "캠페인",
                null,
                10000,
                Map.of(),
                LocalDateTime.now().plusDays(7),
                3
        );
        ReflectionTestUtils.setField(campaign, "campaignId", 100L);

        Wallet wallet = Wallet.create(1L);
        ReflectionTestUtils.setField(wallet, "walletId", 10L);
        ReflectionTestUtils.setField(wallet, "balance", 70000);
        ReflectionTestUtils.setField(wallet, "lockedBalance", 30000);

        EscrowLedger lockedEscrow = EscrowLedger.create(100L, 2L, 10000);
        EscrowLedger releasedEscrow = EscrowLedger.create(100L, 3L, 15000);
        releasedEscrow.release();
        EscrowLedger canceledEscrow = EscrowLedger.create(100L, 4L, 5000);
        canceledEscrow.cancel();

        PointHistory chargeHistory = PointHistory.create(wallet, 50000, PointHistoryType.CHARGE, 7L);
        ReflectionTestUtils.setField(chargeHistory, "historyId", 1L);
        PointHistoryResponse chargeResponse = PointHistoryResponse.from(chargeHistory);

        when(campaignRepository.findByAdvertiserId(1L)).thenReturn(List.of(campaign));
        when(campaignRepository.countByAdvertiserIdAndStatus(1L, CampaignStatus.RECRUITING)).thenReturn(1L);
        when(campaignRepository.countByAdvertiserIdAndStatus(1L, CampaignStatus.IN_PROGRESS)).thenReturn(0L);
        when(campaignRepository.countByAdvertiserIdAndStatus(1L, CampaignStatus.COMPLETED)).thenReturn(0L);
        when(applicationRepository.countByCampaignIdInAndStatus(List.of(100L), ApplicationStatus.PENDING)).thenReturn(2L);
        when(applicationRepository.countByCampaignIdInAndStatus(List.of(100L), ApplicationStatus.ACCEPTED)).thenReturn(1L);
        when(applicationRepository.findRecentByCampaignIdIn(eq(List.of(100L)), any(Pageable.class))).thenReturn(List.of());
        when(missionRepository.countByCampaignIdInAndStatus(List.of(100L), MissionStatus.SUBMITTED)).thenReturn(3L);
        when(missionRepository.countByCampaignIdInAndStatus(List.of(100L), MissionStatus.APPROVED)).thenReturn(4L);
        when(missionRepository.countByCampaignIdInAndStatus(List.of(100L), MissionStatus.REJECTED)).thenReturn(1L);
        when(missionRepository.findByCampaignIdInAndStatus(List.of(100L), MissionStatus.SUBMITTED)).thenReturn(List.of());
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(pointHistoryRepository.findByWallet_WalletId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(chargeHistory), PageRequest.of(0, 5), 1));
        when(pointHistoryResponseMapper.toResponse(chargeHistory)).thenReturn(chargeResponse);
        when(escrowLedgerRepository.findByCampaignIdIn(List.of(100L)))
                .thenReturn(List.of(lockedEscrow, releasedEscrow, canceledEscrow));

        AdvertiserDashboardResponse response = advertiserDashboardService.getDashboard(1L);

        assertThat(response.wallet().balance()).isEqualTo(70000);
        assertThat(response.wallet().lockedBalance()).isEqualTo(30000);
        assertThat(response.escrowSummary().lockedEscrows()).isEqualTo(1);
        assertThat(response.escrowSummary().releasedEscrows()).isEqualTo(1);
        assertThat(response.escrowSummary().canceledEscrows()).isEqualTo(1);
        assertThat(response.escrowSummary().lockedAmount()).isEqualTo(10000);
        assertThat(response.escrowSummary().releasedAmount()).isEqualTo(15000);
        assertThat(response.escrowSummary().canceledAmount()).isEqualTo(5000);
        assertThat(response.recentPointHistories()).hasSize(1);
        assertThat(response.recentPointHistories().get(0).getType()).isEqualTo(PointHistoryType.CHARGE);
        assertThat(response.recentPointHistories().get(0).getReferenceType()).isEqualTo(PointHistoryReferenceType.PAYMENT);
        assertThat(response.recentPointHistories().get(0).getReferenceId()).isEqualTo(7L);
    }
}
