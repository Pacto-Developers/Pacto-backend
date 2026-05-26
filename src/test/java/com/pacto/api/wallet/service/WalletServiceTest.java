package com.pacto.api.wallet.service;

import com.pacto.api.common.exception.InsufficientBalanceException;
import com.pacto.api.common.exception.WalletNotFoundException;
import com.pacto.api.wallet.dto.PointHistoryResponse;
import com.pacto.api.wallet.dto.WalletResponse;
import com.pacto.api.wallet.dto.WithdrawRequest;
import com.pacto.api.wallet.dto.WithdrawResponse;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.entity.Withdrawal;
import com.pacto.api.wallet.entity.WithdrawalStatus;
import com.pacto.api.wallet.repository.PointHistoryRepository;
import com.pacto.api.wallet.repository.WalletRepository;
import com.pacto.api.wallet.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock WalletRepository walletRepository;
    @Mock PointHistoryRepository pointHistoryRepository;
    @Mock WithdrawalRepository withdrawalRepository;
    @InjectMocks WalletService walletService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = Wallet.create(1L);
        ReflectionTestUtils.setField(wallet, "walletId", 10L);
    }

    @Test
    void 지갑_조회_성공() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getMyWallet(1L);

        assertThat(response.getWalletId()).isEqualTo(10L);
        assertThat(response.getBalance()).isEqualTo(0);
        assertThat(response.getLockedBalance()).isEqualTo(0);
    }

    @Test
    void 지갑_없으면_WalletNotFoundException() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getMyWallet(1L))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessage("지갑을 찾을 수 없습니다.");
    }

    @Test
    void 포인트_내역_조회_성공() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(pointHistoryRepository.findByWallet_WalletId(10L)).thenReturn(List.of());

        List<PointHistoryResponse> result = walletService.getMyHistories(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void 출금신청_잔액_충분() {
        ReflectionTestUtils.setField(wallet, "balance", 50000);

        Withdrawal savedWithdrawal = Withdrawal.create(wallet, 30000, "카카오뱅크", "111");
        ReflectionTestUtils.setField(savedWithdrawal, "withdrawalId", 1L);

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(withdrawalRepository.save(any(Withdrawal.class))).thenReturn(savedWithdrawal);

        WithdrawRequest request = new WithdrawRequest();
        ReflectionTestUtils.setField(request, "amount", 30000);
        ReflectionTestUtils.setField(request, "bankName", "카카오뱅크");
        ReflectionTestUtils.setField(request, "accountNumber", "111");

        WithdrawResponse response = walletService.requestWithdraw(1L, request);

        assertThat(response.getAmount()).isEqualTo(30000);
        assertThat(response.getStatus()).isEqualTo(WithdrawalStatus.PENDING);
        assertThat(wallet.getBalance()).isEqualTo(20000);
        verify(walletRepository).save(wallet);
        verify(withdrawalRepository).save(any(Withdrawal.class));
    }

    @Test
    void 출금신청_잔액_부족() {
        // balance 기본값 0
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        WithdrawRequest request = new WithdrawRequest();
        ReflectionTestUtils.setField(request, "amount", 10000);
        ReflectionTestUtils.setField(request, "bankName", "카카오뱅크");
        ReflectionTestUtils.setField(request, "accountNumber", "111");

        assertThatThrownBy(() -> walletService.requestWithdraw(1L, request))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("잔액이 부족합니다.");

        verify(withdrawalRepository, never()).save(any());
    }
}
