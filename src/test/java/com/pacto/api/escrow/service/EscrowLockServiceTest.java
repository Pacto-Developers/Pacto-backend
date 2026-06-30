package com.pacto.api.escrow.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.exception.CampaignNotFoundException;
import com.pacto.api.common.exception.InsufficientBalanceException;
import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.entity.EscrowStatus;
import com.pacto.api.escrow.repository.EscrowLedgerRepository;
import com.pacto.api.wallet.entity.PointHistory;
import com.pacto.api.wallet.entity.PointHistoryType;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.repository.PointHistoryRepository;
import com.pacto.api.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscrowLockServiceTest {

    @Mock EscrowLedgerRepository escrowLedgerRepository;
    @Mock WalletRepository walletRepository;
    @Mock CampaignRepository campaignRepository;
    @Mock PointHistoryRepository pointHistoryRepository;
    @InjectMocks EscrowLockService escrowLockService;

    @Test
    void lockCampaignBudget은_캠페인_총_예산을_잠근다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        Wallet advertiserWallet = Wallet.create(1L);
        ReflectionTestUtils.setField(advertiserWallet, "walletId", 100L);
        ReflectionTestUtils.setField(advertiserWallet, "balance", 200000);

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(advertiserWallet));

        escrowLockService.lockCampaignBudget(campaign);

        assertThat(advertiserWallet.getBalance()).isEqualTo(50000);
        assertThat(advertiserWallet.getLockedBalance()).isEqualTo(150000);

        ArgumentCaptor<PointHistory> historyCaptor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(historyCaptor.capture());
        PointHistory history = historyCaptor.getValue();
        assertThat(history.getWallet()).isSameAs(advertiserWallet);
        assertThat(history.getAmount()).isEqualTo(-150000);
        assertThat(history.getType()).isEqualTo(PointHistoryType.LOCK);
        assertThat(history.getReferenceId()).isEqualTo(10L);
        verify(walletRepository).save(advertiserWallet);
        verify(escrowLedgerRepository, never()).save(any());
    }

    @Test
    void lockCampaignBudget은_광고주_잔액이_부족하면_예산을_잠그지_않는다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        Wallet advertiserWallet = Wallet.create(1L);
        ReflectionTestUtils.setField(advertiserWallet, "walletId", 100L);
        ReflectionTestUtils.setField(advertiserWallet, "balance", 100000);

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(advertiserWallet));

        assertThatThrownBy(() -> escrowLockService.lockCampaignBudget(campaign))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("잔액이 부족합니다.");

        assertThat(advertiserWallet.getBalance()).isEqualTo(100000);
        assertThat(advertiserWallet.getLockedBalance()).isEqualTo(0);
        verify(walletRepository, never()).save(any());
        verify(escrowLedgerRepository, never()).save(any());
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    void createEscrowForSelection은_추가_차감_없이_에스크로만_생성한다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);

        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(escrowLedgerRepository.save(any(EscrowLedger.class))).thenAnswer(invocation -> {
            EscrowLedger escrow = invocation.getArgument(0);
            ReflectionTestUtils.setField(escrow, "escrowId", 505L);
            return escrow;
        });

        Long escrowId = escrowLockService.createEscrowForSelection(10L, 42L);

        assertThat(escrowId).isEqualTo(505L);
        ArgumentCaptor<EscrowLedger> escrowCaptor = ArgumentCaptor.forClass(EscrowLedger.class);
        verify(escrowLedgerRepository).save(escrowCaptor.capture());
        EscrowLedger escrow = escrowCaptor.getValue();
        assertThat(escrow.getCampaignId()).isEqualTo(10L);
        assertThat(escrow.getBloggerId()).isEqualTo(42L);
        assertThat(escrow.getAmount()).isEqualTo(50000);
        assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.LOCKED);
        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    void refundUnusedBudget은_미선정_슬롯_예산을_환불하고_남은_슬롯을_0으로_만든다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        campaign.decreaseSlot();
        Wallet advertiserWallet = Wallet.create(1L);
        ReflectionTestUtils.setField(advertiserWallet, "walletId", 100L);
        ReflectionTestUtils.setField(advertiserWallet, "lockedBalance", 150000);

        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(advertiserWallet));

        escrowLockService.refundUnusedBudget(10L);

        assertThat(campaign.getRemainingSlots()).isEqualTo(0);
        assertThat(advertiserWallet.getBalance()).isEqualTo(100000);
        assertThat(advertiserWallet.getLockedBalance()).isEqualTo(50000);

        ArgumentCaptor<PointHistory> historyCaptor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(historyCaptor.capture());
        PointHistory history = historyCaptor.getValue();
        assertThat(history.getWallet()).isSameAs(advertiserWallet);
        assertThat(history.getAmount()).isEqualTo(100000);
        assertThat(history.getType()).isEqualTo(PointHistoryType.REFUND);
        assertThat(history.getReferenceId()).isEqualTo(10L);
        verify(walletRepository).save(advertiserWallet);
        verify(campaignRepository).save(campaign);
    }

    @Test
    void refundUnusedBudget은_연속_호출해도_미선정_예산을_한_번만_환불한다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        campaign.decreaseSlot();
        Wallet advertiserWallet = Wallet.create(1L);
        ReflectionTestUtils.setField(advertiserWallet, "walletId", 100L);
        ReflectionTestUtils.setField(advertiserWallet, "lockedBalance", 150000);

        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(advertiserWallet));

        escrowLockService.refundUnusedBudget(10L);
        escrowLockService.refundUnusedBudget(10L);

        assertThat(campaign.getRemainingSlots()).isEqualTo(0);
        assertThat(advertiserWallet.getBalance()).isEqualTo(100000);
        assertThat(advertiserWallet.getLockedBalance()).isEqualTo(50000);
        verify(walletRepository).findByUserId(1L);
        verify(walletRepository).save(advertiserWallet);
        verify(campaignRepository).save(campaign);
        verify(pointHistoryRepository).save(any(PointHistory.class));
    }

    @Test
    void refundUnusedBudget은_모든_슬롯이_선정되면_아무것도_저장하지_않는다() {
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 3);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        campaign.decreaseSlot();
        campaign.decreaseSlot();
        campaign.decreaseSlot();

        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

        escrowLockService.refundUnusedBudget(10L);

        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
        verify(pointHistoryRepository, never()).save(any());
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void lock은_캠페인이_없으면_공통_예외를_던진다() {
        when(campaignRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> escrowLockService.createEscrowForSelection(10L, 42L))
                .isInstanceOf(CampaignNotFoundException.class)
                .hasMessage("캠페인을 찾을 수 없습니다.");

        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
        verify(escrowLedgerRepository, never()).save(any());
        verify(pointHistoryRepository, never()).save(any());
    }
}
