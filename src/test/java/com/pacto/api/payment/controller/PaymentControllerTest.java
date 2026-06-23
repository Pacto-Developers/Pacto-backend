package com.pacto.api.payment.controller;

import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.response.CommonResponse;
import com.pacto.api.payment.dto.PaymentResponse;
import com.pacto.api.payment.service.PaymentService;
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
class PaymentControllerTest {

    @Mock PaymentService paymentService;
    @InjectMocks PaymentController paymentController;

    @Test
    void 내_결제_내역_조회는_CommonResponse로_응답한다() {
        PageResponse<PaymentResponse> payments = PageResponse.from(new PageImpl<>(List.of()), PaymentResponse::from);
        when(paymentService.getMyPayments(1L, 1, 20)).thenReturn(payments);

        ResponseEntity<CommonResponse<PageResponse<PaymentResponse>>> response = paymentController.getMyPayments(
                new UsernamePasswordAuthenticationToken(1L, null),
                1,
                20
        );

        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("결제 내역 조회 성공");
        assertThat(response.getBody().data()).isSameAs(payments);
    }
}
