package com.pacto.api.domain.wallet.repository;

import com.pacto.api.domain.wallet.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByWallet_WalletId(Long walletId);
}
