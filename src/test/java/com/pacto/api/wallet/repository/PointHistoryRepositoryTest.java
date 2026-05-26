package com.pacto.api.wallet.repository;

import com.pacto.api.wallet.entity.PointHistory;
import com.pacto.api.wallet.entity.PointHistoryType;
import com.pacto.api.wallet.entity.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PointHistoryRepositoryTest {

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private WalletRepository walletRepository;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = walletRepository.save(Wallet.create(1L));
    }

    @Test
    void 포인트_내역_저장_후_조회() {
        PointHistory history = PointHistory.create(wallet, 10000, PointHistoryType.CHARGE, 42L);
        PointHistory saved = pointHistoryRepository.save(history);

        assertThat(saved.getHistoryId()).isNotNull();
        assertThat(saved.getAmount()).isEqualTo(10000);
        assertThat(saved.getType()).isEqualTo(PointHistoryType.CHARGE);
        assertThat(saved.getReferenceId()).isEqualTo(42L);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void walletId로_포인트_내역_목록_조회() {
        pointHistoryRepository.save(PointHistory.create(wallet, 10000, PointHistoryType.CHARGE, null));
        pointHistoryRepository.save(PointHistory.create(wallet, -5000, PointHistoryType.LOCK, 1L));

        List<PointHistory> histories = pointHistoryRepository.findByWallet_WalletId(wallet.getWalletId());

        assertThat(histories).hasSize(2);
    }

    @Test
    void referenceId_null도_저장_가능() {
        PointHistory history = PointHistory.create(wallet, 10000, PointHistoryType.CHARGE, null);
        PointHistory saved = pointHistoryRepository.save(history);

        assertThat(saved.getReferenceId()).isNull();
    }
}
