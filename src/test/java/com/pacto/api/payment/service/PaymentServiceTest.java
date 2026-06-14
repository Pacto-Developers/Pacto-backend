package com.pacto.api.payment.service;

import com.pacto.api.common.exception.PaymentAlreadyProcessedException;
import com.pacto.api.common.exception.PaymentNotFoundException;
import com.pacto.api.common.exception.PaymentVerificationException;
import com.pacto.api.payment.client.PortOneClient;
import com.pacto.api.payment.client.PortOnePaymentResponse;
import com.pacto.api.payment.dto.PaymentCreateRequest;
import com.pacto.api.payment.dto.PaymentResponse;
import com.pacto.api.payment.dto.PaymentVerifyRequest;
import com.pacto.api.payment.entity.Payment;
import com.pacto.api.payment.entity.PaymentStatus;
import com.pacto.api.payment.repository.PaymentRepository;
import com.pacto.api.wallet.service.WalletService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PortOneClient portOneClient;
    @Mock WalletService walletService;
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
    void 결제_검증_성공시_PAID_상태로_변경하고_지갑을_충전한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        ReflectionTestUtils.setField(payment, "paymentId", 7L);
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        ReflectionTestUtils.setField(request, "merchantUid", "payment-1");
        ReflectionTestUtils.setField(request, "impUid", "imp-1");
        when(paymentRepository.findByMerchantUid("payment-1")).thenReturn(Optional.of(payment));
        when(portOneClient.getPayment("imp-1"))
                .thenReturn(new PortOnePaymentResponse("imp-1", "payment-1", 10000, "paid"));

        PaymentResponse response = paymentService.verifyPayment(1L, request);

        assertThat(response.getMerchantUid()).isEqualTo("payment-1");
        assertThat(response.getImpUid()).isEqualTo("imp-1");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(walletService).chargeByPayment(1L, 10000, 7L);
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

        verifyNoInteractions(portOneClient);
    }

    @Test
    void 이미_처리된_결제는_다시_검증할_수_없다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        payment.markPaid("imp-1");
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        ReflectionTestUtils.setField(request, "merchantUid", "payment-1");
        ReflectionTestUtils.setField(request, "impUid", "imp-1");
        when(paymentRepository.findByMerchantUid("payment-1")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, request))
                .isInstanceOf(PaymentAlreadyProcessedException.class)
                .hasMessage("이미 처리된 결제입니다.");

        verifyNoInteractions(portOneClient);
        verifyNoInteractions(walletService);
    }

    @Test
    void 포트원_결제_요청번호가_다르면_검증에_실패한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        ReflectionTestUtils.setField(request, "merchantUid", "payment-1");
        ReflectionTestUtils.setField(request, "impUid", "imp-1");
        when(paymentRepository.findByMerchantUid("payment-1")).thenReturn(Optional.of(payment));
        when(portOneClient.getPayment("imp-1"))
                .thenReturn(new PortOnePaymentResponse("imp-1", "payment-2", 10000, "paid"));

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, request))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessage("결제 요청 번호가 일치하지 않습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        verifyNoInteractions(walletService);
    }

    @Test
    void 포트원_결제_금액이_다르면_검증에_실패한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        ReflectionTestUtils.setField(request, "merchantUid", "payment-1");
        ReflectionTestUtils.setField(request, "impUid", "imp-1");
        when(paymentRepository.findByMerchantUid("payment-1")).thenReturn(Optional.of(payment));
        when(portOneClient.getPayment("imp-1"))
                .thenReturn(new PortOnePaymentResponse("imp-1", "payment-1", 9000, "paid"));

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, request))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessage("결제 금액이 일치하지 않습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        verifyNoInteractions(walletService);
    }

    @Test
    void 포트원_결제_상태가_paid가_아니면_검증에_실패한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        ReflectionTestUtils.setField(request, "merchantUid", "payment-1");
        ReflectionTestUtils.setField(request, "impUid", "imp-1");
        when(paymentRepository.findByMerchantUid("payment-1")).thenReturn(Optional.of(payment));
        when(portOneClient.getPayment("imp-1"))
                .thenReturn(new PortOnePaymentResponse("imp-1", "payment-1", 10000, "ready"));

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, request))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessage("결제가 완료되지 않았습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        verifyNoInteractions(walletService);
    }

    @Test
    void 요청한_포트원_결제번호와_조회된_결제번호가_다르면_검증에_실패한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        ReflectionTestUtils.setField(request, "merchantUid", "payment-1");
        ReflectionTestUtils.setField(request, "impUid", "imp-1");
        when(paymentRepository.findByMerchantUid("payment-1")).thenReturn(Optional.of(payment));
        when(portOneClient.getPayment("imp-1"))
                .thenReturn(new PortOnePaymentResponse("imp-2", "payment-1", 10000, "paid"));

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, request))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessage("포트원 결제 번호가 일치하지 않습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(walletService);
    }
}
