package com.pacto.api.wallet.controller;

import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.response.CommonResponse;
import com.pacto.api.wallet.dto.PointHistoryResponse;
import com.pacto.api.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock WalletService walletService;
    @InjectMocks WalletController walletController;

    @Test
    void 포인트_내역_조회는_CommonResponse로_응답한다() {
        PageResponse<PointHistoryResponse> histories = PageResponse.from(
                new PageImpl<>(List.of()),
                PointHistoryResponse::from
        );
        when(walletService.getMyHistories(1L, 1, 20)).thenReturn(histories);

        ResponseEntity<?> response = walletController.getMyHistories(
                new UsernamePasswordAuthenticationToken(1L, null),
                1,
                20
        );

        assertThat(response.getBody()).isInstanceOf(CommonResponse.class);
        CommonResponse<?> body = (CommonResponse<?>) response.getBody();
        assertThat(body.success()).isTrue();
        assertThat(body.message()).isEqualTo("포인트 내역 조회 성공");
        assertThat(body.data()).isSameAs(histories);
        assertThat(histories.getContent()).isEmpty();
        assertThat(histories.getTotalPages()).isEqualTo(1);
        assertThat(histories.getCurrentPage()).isEqualTo(1);
    }
}
