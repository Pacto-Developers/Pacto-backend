package com.pacto.api.domain.wallet.repository;

import com.pacto.api.domain.wallet.entity.Wallet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class WalletRepositoryTest {

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void 지갑_저장_후_조회() {
        Wallet wallet = Wallet.create(1L);
        Wallet saved = walletRepository.save(wallet);

        Optional<Wallet> found = walletRepository.findById(saved.getWalletId());

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
        assertThat(found.get().getBalance()).isEqualTo(0);
        assertThat(found.get().getLockedBalance()).isEqualTo(0);
    }

    @Test
    void userId로_지갑_조회() {
        walletRepository.save(Wallet.create(2L));

        Optional<Wallet> found = walletRepository.findByUserId(2L);

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(2L);
    }

    @Test
    void 존재하지_않는_userId로_조회시_빈_결과() {
        Optional<Wallet> found = walletRepository.findByUserId(999L);
        assertThat(found).isEmpty();
    }
}
