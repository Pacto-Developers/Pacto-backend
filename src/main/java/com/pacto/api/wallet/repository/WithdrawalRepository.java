package com.pacto.api.wallet.repository;

import com.pacto.api.wallet.entity.Withdrawal;
import com.pacto.api.wallet.entity.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findByWallet_WalletId(Long walletId);
    List<Withdrawal> findByStatus(WithdrawalStatus status);
}
