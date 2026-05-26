package com.pacto.api.escrow.repository;

import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.entity.EscrowStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EscrowLedgerRepositoryTest {

    @Autowired
    private EscrowLedgerRepository escrowLedgerRepository;

    @Test
    void 에스크로_저장_후_조회() {
        EscrowLedger escrow = EscrowLedger.create(10L, 20L, 50000);
        EscrowLedger saved = escrowLedgerRepository.save(escrow);

        Optional<EscrowLedger> found = escrowLedgerRepository.findById(saved.getEscrowId());

        assertThat(found).isPresent();
        assertThat(found.get().getCampaignId()).isEqualTo(10L);
        assertThat(found.get().getBloggerId()).isEqualTo(20L);
        assertThat(found.get().getAmount()).isEqualTo(50000);
        assertThat(found.get().getStatus()).isEqualTo(EscrowStatus.LOCKED);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void campaignId로_에스크로_목록_조회() {
        escrowLedgerRepository.save(EscrowLedger.create(10L, 20L, 50000));
        escrowLedgerRepository.save(EscrowLedger.create(10L, 21L, 50000));
        escrowLedgerRepository.save(EscrowLedger.create(11L, 22L, 50000));

        List<EscrowLedger> result = escrowLedgerRepository.findByCampaignId(10L);

        assertThat(result).hasSize(2);
    }

    @Test
    void campaignId와_status로_에스크로_조회() {
        escrowLedgerRepository.save(EscrowLedger.create(10L, 20L, 50000));

        List<EscrowLedger> result = escrowLedgerRepository.findByCampaignIdAndStatus(10L, EscrowStatus.LOCKED);

        assertThat(result).hasSize(1);
    }

    @Test
    void bloggerId로_에스크로_목록_조회() {
        escrowLedgerRepository.save(EscrowLedger.create(10L, 20L, 50000));
        escrowLedgerRepository.save(EscrowLedger.create(11L, 20L, 30000));

        List<EscrowLedger> result = escrowLedgerRepository.findByBloggerId(20L);

        assertThat(result).hasSize(2);
    }
}
