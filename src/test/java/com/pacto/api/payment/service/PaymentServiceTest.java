package com.pacto.api.payment.service;

import com.pacto.api.common.exception.PaymentNotFoundException;
import com.pacto.api.payment.dto.PaymentCreateRequest;
import com.pacto.api.payment.dto.PaymentResponse;
import com.pacto.api.payment.dto.PaymentVerifyRequest;
import com.pacto.api.payment.entity.Payment;
import com.pacto.api.payment.entity.PaymentStatus;
import com.pacto.api.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @InjectMocks PaymentService paymentService;

    @Test
    void 결제_요청을_READY_상태로_생성한다() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        ReflectionTestUtils.setField(request, "amount", 10000);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.createPayment(1L, request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getMerchantUid()).startsWith("payment_");
        assertThat(response.getAmount()).isEqualTo(10000);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.READY);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getUserId()).isEqualTo(1L);
    }

    @Test
    void 결제_검증_요청은_내_결제_요청만_조회한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        ReflectionTestUtils.setField(request, "merchantUid", "payment-1");
        ReflectionTestUtils.setField(request, "impUid", "imp-1");
        when(paymentRepository.findByMerchantUid("payment-1")).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.verifyPayment(1L, request);

        assertThat(response.getMerchantUid()).isEqualTo("payment-1");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.READY);
    }

    @Test
    void 다른_사용자의_결제_요청은_검증할_수_없다() {
        Payment payment = Payment.createReady(2L, "payment-1", 10000);
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        ReflectionTestUtils.setField(request, "merchantUid", "payment-1");
        when(paymentRepository.findByMerchantUid("payment-1")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, request))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("결제 요청을 찾을 수 없습니다.");
    }
}
