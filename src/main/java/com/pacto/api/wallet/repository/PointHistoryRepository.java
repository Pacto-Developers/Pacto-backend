package com.pacto.api.wallet.repository;

import com.pacto.api.wallet.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByWallet_WalletId(Long walletId);
    Page<PointHistory> findByWallet_WalletId(Long walletId, Pageable pageable);
}
