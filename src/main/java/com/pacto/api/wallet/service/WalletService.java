package com.pacto.api.wallet.service;

import com.pacto.api.common.exception.InsufficientBalanceException;
import com.pacto.api.common.exception.WalletNotFoundException;
import com.pacto.api.wallet.dto.PointHistoryResponse;
import com.pacto.api.wallet.dto.WalletResponse;
import com.pacto.api.wallet.dto.WithdrawRequest;
import com.pacto.api.wallet.dto.WithdrawResponse;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.entity.Withdrawal;
import com.pacto.api.wallet.repository.PointHistoryRepository;
import com.pacto.api.wallet.repository.WalletRepository;
import com.pacto.api.wallet.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    public List<PointHistoryResponse> getMyHistories(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);
        return pointHistoryRepository.findByWallet_WalletId(wallet.getWalletId())
                .stream()
                .map(PointHistoryResponse::from)
                .collect(Collectors.toList());
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
        return WithdrawResponse.from(withdrawalRepository.save(withdrawal));
    }
}
