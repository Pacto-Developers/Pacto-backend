package com.pacto.api.escrow.service;

import com.pacto.api.escrow.dto.EscrowLedgerResponse;
import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.entity.EscrowStatus;
import com.pacto.api.escrow.repository.EscrowLedgerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscrowServiceTest {

    @Mock EscrowLedgerRepository escrowLedgerRepository;
    @InjectMocks EscrowService escrowService;

    @Test
    void 내_에스크로_목록_조회() {
        EscrowLedger escrow = EscrowLedger.create(10L, 1L, 50000);
        when(escrowLedgerRepository.findByBloggerId(1L)).thenReturn(List.of(escrow));

        List<EscrowLedgerResponse> result = escrowService.getMyEscrows(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCampaignId()).isEqualTo(10L);
        assertThat(result.get(0).getAmount()).isEqualTo(50000);
        assertThat(result.get(0).getStatus()).isEqualTo(EscrowStatus.LOCKED);
    }

    @Test
    void 에스크로_없으면_빈_리스트() {
        when(escrowLedgerRepository.findByBloggerId(1L)).thenReturn(List.of());

        List<EscrowLedgerResponse> result = escrowService.getMyEscrows(1L);

        assertThat(result).isEmpty();
    }
}
