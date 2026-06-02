package com.pacto.api.wallet.service;

import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.exception.InsufficientBalanceException;
import com.pacto.api.common.exception.WalletNotFoundException;
import com.pacto.api.wallet.dto.PointHistoryResponse;
import com.pacto.api.wallet.dto.WalletResponse;
import com.pacto.api.wallet.dto.WithdrawRequest;
import com.pacto.api.wallet.dto.WithdrawResponse;
import com.pacto.api.wallet.entity.PointHistory;
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

    private final WalletRepository walletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final WithdrawalRepository withdrawalRepository;

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
                PointHistoryResponse::from
        );
    }

    @Transactional
    public WithdrawResponse requestWithdraw(Long userId, WithdrawRequest request) {
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
                savedWithdrawal.getWithdrawalId()
        ));

        return WithdrawResponse.from(savedWithdrawal, wallet.getBalance());
    }
}
