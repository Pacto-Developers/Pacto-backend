package com.pacto.api.wallet.service;

import com.pacto.api.common.exception.InsufficientBalanceException;
import com.pacto.api.common.exception.InvalidChargeAmountException;
import com.pacto.api.common.exception.InvalidWithdrawalAmountException;
import com.pacto.api.common.exception.WalletNotFoundException;
import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.wallet.dto.PointHistoryResponse;
import com.pacto.api.wallet.dto.WalletResponse;
import com.pacto.api.wallet.dto.WithdrawRequest;
import com.pacto.api.wallet.dto.WithdrawResponse;
import com.pacto.api.wallet.entity.PointHistory;
import com.pacto.api.wallet.entity.PointHistoryType;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.entity.Withdrawal;
import com.pacto.api.wallet.entity.WithdrawalStatus;
import com.pacto.api.wallet.repository.PointHistoryRepository;
import com.pacto.api.wallet.repository.WalletRepository;
import com.pacto.api.wallet.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
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
    @Mock PointHistoryResponseMapper pointHistoryResponseMapper;
    @InjectMocks WalletService walletService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = Wallet.create(1L);
        ReflectionTestUtils.setField(wallet, "walletId", 10L);
    }

    @Test
    void 지갑_조회_성공() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 19, 10, 0);
        ReflectionTestUtils.setField(wallet, "updatedAt", updatedAt);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getMyWallet(1L);

        assertThat(response.getWalletId()).isEqualTo(10L);
        assertThat(response.getBalance()).isEqualTo(0);
        assertThat(response.getLockedBalance()).isEqualTo(0);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void 지갑_없으면_WalletNotFoundException() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getMyWallet(1L))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessage("지갑을 찾을 수 없습니다.");
    }

    @Test
    void 포인트_내역은_최신순_페이지로_조회() {
        PointHistory history = PointHistory.create(wallet, -50000, PointHistoryType.LOCK, 505L);
        ReflectionTestUtils.setField(history, "historyId", 102L);
        ReflectionTestUtils.setField(history, "createdAt", LocalDateTime.of(2026, 5, 19, 9, 30));

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(pointHistoryRepository.findByWallet_WalletId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(history), PageRequest.of(0, 20), 1));
        when(pointHistoryResponseMapper.toResponse(history)).thenReturn(PointHistoryResponse.from(history));

        PageResponse<PointHistoryResponse> result = walletService.getMyHistories(1L, 1, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getHistoryId()).isEqualTo(102L);
        assertThat(result.getContent().get(0).getReferenceId()).isEqualTo(505L);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(pointHistoryRepository).findByWallet_WalletId(eq(10L), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
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

        assertThat(response.getWithdrawalId()).isEqualTo(1L);
        assertThat(response.getRequestedAmount()).isEqualTo(30000);
        assertThat(response.getRemainingBalance()).isEqualTo(20000);
        assertThat(response.getStatus()).isEqualTo(WithdrawalStatus.PENDING);
        assertThat(wallet.getBalance()).isEqualTo(20000);
        verify(walletRepository).save(wallet);
        verify(withdrawalRepository).save(any(Withdrawal.class));

        ArgumentCaptor<PointHistory> historyCaptor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(historyCaptor.capture());
        PointHistory history = historyCaptor.getValue();
        assertThat(history.getAmount()).isEqualTo(-30000);
        assertThat(history.getType()).isEqualTo(PointHistoryType.WITHDRAW);
        assertThat(history.getReferenceId()).isEqualTo(1L);
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

    @ParameterizedTest
    @ValueSource(ints = {-10000, 0, 9999})
    void 출금신청은_10000원_미만이면_거부한다(int amount) {
        WithdrawRequest request = new WithdrawRequest();
        ReflectionTestUtils.setField(request, "amount", amount);
        ReflectionTestUtils.setField(request, "bankName", "카카오뱅크");
        ReflectionTestUtils.setField(request, "accountNumber", "111");

        assertThatThrownBy(() -> walletService.requestWithdraw(1L, request))
                .isInstanceOf(InvalidWithdrawalAmountException.class)
                .hasMessage("출금 금액은 10,000원 이상이어야 합니다.");

        verifyNoInteractions(walletRepository, withdrawalRepository, pointHistoryRepository);
    }

    @Test
    void 출금신청은_정확히_10000원이면_허용한다() {
        ReflectionTestUtils.setField(wallet, "balance", 10000);
        Withdrawal savedWithdrawal = Withdrawal.create(wallet, 10000, "카카오뱅크", "111");
        ReflectionTestUtils.setField(savedWithdrawal, "withdrawalId", 1L);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(withdrawalRepository.save(any(Withdrawal.class))).thenReturn(savedWithdrawal);

        WithdrawRequest request = new WithdrawRequest();
        ReflectionTestUtils.setField(request, "amount", 10000);
        ReflectionTestUtils.setField(request, "bankName", "카카오뱅크");
        ReflectionTestUtils.setField(request, "accountNumber", "111");

        WithdrawResponse response = walletService.requestWithdraw(1L, request);

        assertThat(response.getRequestedAmount()).isEqualTo(10000);
        assertThat(response.getRemainingBalance()).isZero();
        verify(walletRepository).save(wallet);
        verify(withdrawalRepository).save(any(Withdrawal.class));
        verify(pointHistoryRepository).save(any(PointHistory.class));
    }

    @Test
    void 결제충전_성공() {
        ReflectionTestUtils.setField(wallet, "balance", 10000);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        walletService.chargeByPayment(1L, 30000, 7L);

        assertThat(wallet.getBalance()).isEqualTo(40000);
        verify(walletRepository).save(wallet);

        ArgumentCaptor<PointHistory> historyCaptor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(historyCaptor.capture());
        PointHistory history = historyCaptor.getValue();
        assertThat(history.getWallet()).isSameAs(wallet);
        assertThat(history.getAmount()).isEqualTo(30000);
        assertThat(history.getType()).isEqualTo(PointHistoryType.CHARGE);
        assertThat(history.getReferenceId()).isEqualTo(7L);
    }

    @Test
    void 결제충전_지갑이_없으면_WalletNotFoundException() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.chargeByPayment(1L, 30000, 7L))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessage("지갑을 찾을 수 없습니다.");

        verify(walletRepository, never()).save(any());
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    void 결제충전_금액이_0이하면_InvalidChargeAmountException() {
        assertThatThrownBy(() -> walletService.chargeByPayment(1L, 0, 7L))
                .isInstanceOf(InvalidChargeAmountException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");

        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
        verify(pointHistoryRepository, never()).save(any());
    }
}
