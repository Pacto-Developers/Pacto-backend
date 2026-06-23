package com.pacto.api.escrow.service;

import com.pacto.api.auth.entity.Role;
import com.pacto.api.auth.entity.User;
import com.pacto.api.auth.repository.UserRepository;
import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EscrowServiceTest {

    @Mock EscrowLedgerRepository escrowLedgerRepository;
    @Mock CampaignRepository campaignRepository;
    @Mock UserRepository userRepository;
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

    @Test
    void 광고주_본인_캠페인의_에스크로_목록은_표시정보를_포함한다() {
        Campaign campaign = new Campaign(1L, "광고주 캠페인", null, 50000, Map.of(), LocalDateTime.now(), 2);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        EscrowLedger escrow = EscrowLedger.create(10L, 42L, 50000);
        ReflectionTestUtils.setField(escrow, "escrowId", 505L);
        User blogger = User.builder()
                .userId(42L)
                .email("blogger@example.com")
                .password("password")
                .role(Role.BLOGGER)
                .build();

        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(escrowLedgerRepository.findByCampaignId(10L)).thenReturn(List.of(escrow));
        when(userRepository.findById(42L)).thenReturn(Optional.of(blogger));

        List<EscrowLedgerResponse> result = escrowService.getAdvertiserCampaignEscrows(1L, 10L);

        assertThat(result).hasSize(1);
        EscrowLedgerResponse response = result.get(0);
        assertThat(response.getEscrowId()).isEqualTo(505L);
        assertThat(response.getCampaignId()).isEqualTo(10L);
        assertThat(response.getCampaignTitle()).isEqualTo("광고주 캠페인");
        assertThat(response.getBloggerId()).isEqualTo(42L);
        assertThat(response.getBloggerName()).isEqualTo("blogger@example.com");
        assertThat(response.getBloggerEmail()).isEqualTo("blogger@example.com");
        assertThat(response.getAmount()).isEqualTo(50000);
        assertThat(response.getStatus()).isEqualTo(EscrowStatus.LOCKED);
    }

    @Test
    void 타_광고주_캠페인_에스크로는_조회할_수_없다() {
        Campaign campaign = new Campaign(2L, "다른 캠페인", null, 50000, Map.of(), LocalDateTime.now(), 1);
        ReflectionTestUtils.setField(campaign, "campaignId", 10L);
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> escrowService.getAdvertiserCampaignEscrows(1L, 10L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
