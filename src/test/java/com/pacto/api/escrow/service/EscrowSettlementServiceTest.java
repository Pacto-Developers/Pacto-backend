package com.pacto.api.escrow.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.exception.CampaignNotFoundException;
import com.pacto.api.common.exception.EscrowNotFoundException;
import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.entity.EscrowStatus;
import com.pacto.api.escrow.exception.InvalidEscrowStateException;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscrowSettlementServiceTest {

    @Mock EscrowLedgerRepository escrowLedgerRepository;
    @Mock WalletRepository walletRepository;
    @Mock CampaignRepository campaignRepository;
    @Mock PointHistoryRepository pointHistoryRepository;
    @InjectMocks EscrowSettlementService escrowSettlementService;

    @Test
    void release는_블로거_잔액을_증가시키고_광고주_잠금잔액을_감소시킨다() {
        EscrowLedger escrow = EscrowLedger.create(10L, 42L, 50000);
        ReflectionTestUtils.setField(escrow, "escrowId", 505L);
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 1);
        Wallet advertiserWallet = Wallet.create(1L);
        ReflectionTestUtils.setField(advertiserWallet, "walletId", 100L);
        ReflectionTestUtils.setField(advertiserWallet, "lockedBalance", 50000);
        Wallet bloggerWallet = Wallet.create(42L);
        ReflectionTestUtils.setField(bloggerWallet, "walletId", 200L);

        when(escrowLedgerRepository.findById(505L)).thenReturn(Optional.of(escrow));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(walletRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(advertiserWallet));
        when(walletRepository.findWithLockByUserId(42L)).thenReturn(Optional.of(bloggerWallet));

        escrowSettlementService.release(505L);

        assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.RELEASED);
        assertThat(advertiserWallet.getLockedBalance()).isEqualTo(0);
        assertThat(bloggerWallet.getBalance()).isEqualTo(50000);

        ArgumentCaptor<PointHistory> historyCaptor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(historyCaptor.capture());
        PointHistory history = historyCaptor.getValue();
        assertThat(history.getWallet()).isSameAs(bloggerWallet);
        assertThat(history.getAmount()).isEqualTo(50000);
        assertThat(history.getType()).isEqualTo(PointHistoryType.RELEASE);
        assertThat(history.getReferenceId()).isEqualTo(505L);
        verify(escrowLedgerRepository).save(escrow);
        verify(walletRepository).save(advertiserWallet);
        verify(walletRepository).save(bloggerWallet);
    }

    @Test
    void cancel은_광고주_가용잔액을_복구하고_잠금잔액을_감소시킨다() {
        EscrowLedger escrow = EscrowLedger.create(10L, 42L, 50000);
        ReflectionTestUtils.setField(escrow, "escrowId", 505L);
        Campaign campaign = new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now(), 1);
        Wallet advertiserWallet = Wallet.create(1L);
        ReflectionTestUtils.setField(advertiserWallet, "walletId", 100L);
        ReflectionTestUtils.setField(advertiserWallet, "balance", 100000);
        ReflectionTestUtils.setField(advertiserWallet, "lockedBalance", 50000);

        when(escrowLedgerRepository.findById(505L)).thenReturn(Optional.of(escrow));
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(walletRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(advertiserWallet));

        escrowSettlementService.cancel(505L);

        assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.CANCELED);
        assertThat(advertiserWallet.getBalance()).isEqualTo(150000);
        assertThat(advertiserWallet.getLockedBalance()).isEqualTo(0);

        ArgumentCaptor<PointHistory> historyCaptor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(historyCaptor.capture());
        PointHistory history = historyCaptor.getValue();
        assertThat(history.getWallet()).isSameAs(advertiserWallet);
        assertThat(history.getAmount()).isEqualTo(50000);
        assertThat(history.getType()).isEqualTo(PointHistoryType.REFUND);
        assertThat(history.getReferenceId()).isEqualTo(505L);
        verify(escrowLedgerRepository).save(escrow);
        verify(walletRepository).save(advertiserWallet);
    }

    @Test
    void release는_LOCKED가_아니면_중복_처리를_막는다() {
        EscrowLedger escrow = EscrowLedger.create(10L, 42L, 50000);
        escrow.release();

        when(escrowLedgerRepository.findById(505L)).thenReturn(Optional.of(escrow));

        assertThatThrownBy(() -> escrowSettlementService.release(505L))
                .isInstanceOf(InvalidEscrowStateException.class)
                .hasMessage("LOCKED 상태의 에스크로만 처리할 수 있습니다.");

        verify(escrowLedgerRepository, never()).save(any());
        verifyNoInteractions(campaignRepository, walletRepository, pointHistoryRepository);
    }

    @Test
    void cancel은_LOCKED가_아니면_중복_처리를_막는다() {
        EscrowLedger escrow = EscrowLedger.create(10L, 42L, 50000);
        escrow.cancel();

        when(escrowLedgerRepository.findById(505L)).thenReturn(Optional.of(escrow));

        assertThatThrownBy(() -> escrowSettlementService.cancel(505L))
                .isInstanceOf(InvalidEscrowStateException.class)
                .hasMessage("LOCKED 상태의 에스크로만 처리할 수 있습니다.");

        verify(escrowLedgerRepository, never()).save(any());
        verifyNoInteractions(campaignRepository, walletRepository, pointHistoryRepository);
    }

    @Test
    void cancel은_RELEASED이면_교차_중복_처리를_막는다() {
        EscrowLedger escrow = EscrowLedger.create(10L, 42L, 50000);
        escrow.release();

        when(escrowLedgerRepository.findById(505L)).thenReturn(Optional.of(escrow));

        assertThatThrownBy(() -> escrowSettlementService.cancel(505L))
                .isInstanceOf(InvalidEscrowStateException.class)
                .hasMessage("LOCKED 상태의 에스크로만 처리할 수 있습니다.");

        verify(escrowLedgerRepository, never()).save(any());
        verifyNoInteractions(campaignRepository, walletRepository, pointHistoryRepository);
    }

    @Test
    void release는_CANCELED이면_교차_중복_처리를_막는다() {
        EscrowLedger escrow = EscrowLedger.create(10L, 42L, 50000);
        escrow.cancel();

        when(escrowLedgerRepository.findById(505L)).thenReturn(Optional.of(escrow));

        assertThatThrownBy(() -> escrowSettlementService.release(505L))
                .isInstanceOf(InvalidEscrowStateException.class)
                .hasMessage("LOCKED 상태의 에스크로만 처리할 수 있습니다.");

        verify(escrowLedgerRepository, never()).save(any());
        verifyNoInteractions(campaignRepository, walletRepository, pointHistoryRepository);
    }

    @Test
    void release는_에스크로가_없으면_공통_예외를_던진다() {
        when(escrowLedgerRepository.findById(505L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> escrowSettlementService.release(505L))
                .isInstanceOf(EscrowNotFoundException.class)
                .hasMessage("에스크로를 찾을 수 없습니다.");

        verifyNoInteractions(campaignRepository, walletRepository, pointHistoryRepository);
    }

    @Test
    void release는_캠페인이_없으면_공통_예외를_던진다() {
        EscrowLedger escrow = EscrowLedger.create(10L, 42L, 50000);
        ReflectionTestUtils.setField(escrow, "escrowId", 505L);

        when(escrowLedgerRepository.findById(505L)).thenReturn(Optional.of(escrow));
        when(campaignRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> escrowSettlementService.release(505L))
                .isInstanceOf(CampaignNotFoundException.class)
                .hasMessage("캠페인을 찾을 수 없습니다.");

        verifyNoInteractions(walletRepository, pointHistoryRepository);
    }
}
