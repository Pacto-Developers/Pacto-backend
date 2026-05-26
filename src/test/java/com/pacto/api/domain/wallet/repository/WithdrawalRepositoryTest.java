package com.pacto.api.domain.wallet.repository;

import com.pacto.api.domain.wallet.entity.Wallet;
import com.pacto.api.domain.wallet.entity.Withdrawal;
import com.pacto.api.domain.wallet.entity.WithdrawalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class WithdrawalRepositoryTest {

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private WalletRepository walletRepository;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = walletRepository.save(Wallet.create(1L));
    }

    @Test
    void 출금신청_저장_후_조회() {
        Withdrawal withdrawal = Withdrawal.create(wallet, 50000, "카카오뱅크", "123-456-789012");
        Withdrawal saved = withdrawalRepository.save(withdrawal);

        assertThat(saved.getWithdrawalId()).isNotNull();
        assertThat(saved.getAmount()).isEqualTo(50000);
        assertThat(saved.getBankName()).isEqualTo("카카오뱅크");
        assertThat(saved.getAccountNumber()).isEqualTo("123-456-789012");
        assertThat(saved.getStatus()).isEqualTo(WithdrawalStatus.PENDING);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void walletId로_출금신청_목록_조회() {
        withdrawalRepository.save(Withdrawal.create(wallet, 10000, "카카오뱅크", "111"));
        withdrawalRepository.save(Withdrawal.create(wallet, 20000, "신한은행", "222"));

        List<Withdrawal> result = withdrawalRepository.findByWallet_WalletId(wallet.getWalletId());

        assertThat(result).hasSize(2);
    }

    @Test
    void status로_출금신청_목록_조회() {
        withdrawalRepository.save(Withdrawal.create(wallet, 10000, "카카오뱅크", "111"));

        List<Withdrawal> result = withdrawalRepository.findByStatus(WithdrawalStatus.PENDING);

        assertThat(result).hasSize(1);
    }

    @Test
    void 존재하지_않는_status로_조회시_빈_결과() {
        withdrawalRepository.save(Withdrawal.create(wallet, 10000, "카카오뱅크", "111"));

        List<Withdrawal> result = withdrawalRepository.findByStatus(WithdrawalStatus.COMPLETED);

        assertThat(result).isEmpty();
    }
}
