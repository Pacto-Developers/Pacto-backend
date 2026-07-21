package com.pacto.api.wallet.service;

import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.exception.InsufficientBalanceException;
import com.pacto.api.common.exception.InvalidChargeAmountException;
import com.pacto.api.common.exception.InvalidWithdrawalAmountException;
import com.pacto.api.common.exception.WalletNotFoundException;
import com.pacto.api.wallet.dto.PointHistoryResponse;
import com.pacto.api.wallet.dto.WalletResponse;
import com.pacto.api.wallet.dto.WithdrawRequest;
import com.pacto.api.wallet.dto.WithdrawResponse;
import com.pacto.api.wallet.entity.PointHistory;
import com.pacto.api.wallet.entity.PointHistoryReferenceType;
import com.pacto.api.wallet.entity.PointHistoryType;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.entity.Withdrawal;
import com.pacto.api.wallet.repository.PointHistoryRepository;
import com.pacto.api.wallet.repository.WalletRepository;
import com.pacto.api.wallet.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final int MIN_WITHDRAWAL_AMOUNT = 10_000;

    private final WalletRepository walletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final PointHistoryResponseMapper pointHistoryResponseMapper;

    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);
        return WalletResponse.from(wallet);
    }

    @Transactional(readOnly = true)
    public PageResponse<PointHistoryResponse> getMyHistories(Long userId, int page, int size) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 1) - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return PageResponse.from(
                pointHistoryRepository.findByWallet_WalletId(wallet.getWalletId(), pageRequest),
                pointHistoryResponseMapper::toResponse
        );
    }

    @Transactional
    public WithdrawResponse requestWithdraw(Long userId, WithdrawRequest request) {
        if (request.getAmount() < MIN_WITHDRAWAL_AMOUNT) {
            throw new InvalidWithdrawalAmountException();
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);

        if (wallet.getBalance() < request.getAmount()) {
            throw new InsufficientBalanceException();
        }

        wallet.deductBalance(request.getAmount());
        walletRepository.save(wallet);

        Withdrawal withdrawal = Withdrawal.create(
                wallet, request.getAmount(), request.getBankName(), request.getAccountNumber()
        );
        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);
        pointHistoryRepository.save(PointHistory.create(
                wallet,
                -request.getAmount(),
                PointHistoryType.WITHDRAW,
                savedWithdrawal.getWithdrawalId(),
                PointHistoryReferenceType.WITHDRAWAL
        ));

        return WithdrawResponse.from(savedWithdrawal, wallet.getBalance());
    }

    @Transactional
    public void chargeByPayment(Long userId, int amount, Long paymentId) {
        if (amount <= 0) {
            throw new InvalidChargeAmountException();
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);

        wallet.addBalance(amount);
        walletRepository.save(wallet);
        pointHistoryRepository.save(PointHistory.create(
                wallet,
                amount,
                PointHistoryType.CHARGE,
                paymentId,
                PointHistoryReferenceType.PAYMENT
        ));
    }

    @Transactional
    public void deductByPaymentRefund(Long userId, int amount, Long paymentRefundId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }

        Wallet wallet = walletRepository.findWithLockByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);

        wallet.deductForPaymentRefund(amount);
        walletRepository.save(wallet);
        pointHistoryRepository.save(PointHistory.create(
                wallet,
                -amount,
                PointHistoryType.PAYMENT_REFUND,
                paymentRefundId,
                PointHistoryReferenceType.PAYMENT_REFUND
        ));
    }
}
