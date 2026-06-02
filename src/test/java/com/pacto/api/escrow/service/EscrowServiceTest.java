package com.pacto.api.escrow.service;

import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.escrow.dto.EscrowLedgerResponse;
import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.entity.EscrowStatus;
import com.pacto.api.escrow.repository.EscrowLedgerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EscrowServiceTest {

    @Mock EscrowLedgerRepository escrowLedgerRepository;
    @InjectMocks EscrowService escrowService;

    @Test
    void 내_에스크로_목록은_상태_필터와_페이지로_조회() {
        EscrowLedger escrow = EscrowLedger.create(10L, 1L, 50000);
        when(escrowLedgerRepository.findByBloggerIdAndStatus(eq(1L), eq(EscrowStatus.LOCKED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(escrow), PageRequest.of(0, 20), 1));

        PageResponse<EscrowLedgerResponse> result = escrowService.getMyEscrows(1L, EscrowStatus.LOCKED, 1, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCampaignId()).isEqualTo(10L);
        assertThat(result.getContent().get(0).getAmount()).isEqualTo(50000);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(EscrowStatus.LOCKED);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(escrowLedgerRepository).findByBloggerIdAndStatus(eq(1L), eq(EscrowStatus.LOCKED), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void 상태_필터가_없으면_전체_에스크로_목록_조회() {
        when(escrowLedgerRepository.findByBloggerId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 20), 0));

        PageResponse<EscrowLedgerResponse> result = escrowService.getMyEscrows(1L, null, 1, 20);

        assertThat(result.getContent()).isEmpty();
        verify(escrowLedgerRepository).findByBloggerId(eq(1L), any(Pageable.class));
    }
}
