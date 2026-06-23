package com.pacto.api.wallet.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.repository.EscrowLedgerRepository;
import com.pacto.api.wallet.dto.PointHistoryResponse;
import com.pacto.api.wallet.entity.PointHistory;
import com.pacto.api.wallet.entity.PointHistoryReferenceType;
import com.pacto.api.wallet.entity.PointHistoryType;
import com.pacto.api.wallet.entity.Wallet;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointHistoryResponseMapperTest {

    @Mock CampaignRepository campaignRepository;
    @Mock EscrowLedgerRepository escrowLedgerRepository;
    @InjectMocks PointHistoryResponseMapper mapper;

    @Test
    void 캠페인_reference는_캠페인명과_referenceType을_포함한다() {
        Wallet wallet = Wallet.create(1L);
        PointHistory history = PointHistory.create(
                wallet, -150000, PointHistoryType.LOCK, 10L, PointHistoryReferenceType.CAMPAIGN
        );
        ReflectionTestUtils.setField(history, "historyId", 1L);
        ReflectionTestUtils.setField(history, "createdAt", LocalDateTime.of(2026, 6, 23, 10, 0));
        Campaign campaign = new Campaign(1L, "여름 캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

        PointHistoryResponse response = mapper.toResponse(history);

        assertThat(response.getReferenceType()).isEqualTo(PointHistoryReferenceType.CAMPAIGN);
        assertThat(response.getCampaignId()).isEqualTo(10L);
        assertThat(response.getCampaignTitle()).isEqualTo("여름 캠페인");
    }

    @Test
    void 에스크로_reference는_에스크로의_캠페인명을_포함한다() {
        Wallet wallet = Wallet.create(42L);
        PointHistory history = PointHistory.create(
                wallet, 50000, PointHistoryType.RELEASE, 505L, PointHistoryReferenceType.ESCROW
        );
        EscrowLedger escrow = EscrowLedger.create(10L, 42L, 50000);
        Campaign campaign = new Campaign(1L, "정산 캠페인", null, 50000, Map.of(), LocalDateTime.now(), 1);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        when(escrowLedgerRepository.findById(505L)).thenReturn(Optional.of(escrow));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

        PointHistoryResponse response = mapper.toResponse(history);

        assertThat(response.getReferenceType()).isEqualTo(PointHistoryReferenceType.ESCROW);
        assertThat(response.getCampaignId()).isEqualTo(10L);
        assertThat(response.getCampaignTitle()).isEqualTo("정산 캠페인");
    }
}
