package com.pacto.api.payment.service;

import com.pacto.api.common.exception.InvalidPaymentPageRequestException;
import com.pacto.api.common.exception.PaymentNotFoundException;
import com.pacto.api.common.exception.PaymentVerificationException;
import com.pacto.api.payment.client.PortOneClient;
import com.pacto.api.payment.client.PortOnePaymentResponse;
import com.pacto.api.payment.dto.PaymentCreateRequest;
import com.pacto.api.payment.dto.PaymentDetailResponse;
import com.pacto.api.payment.dto.PaymentResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    void 내_결제_내역을_최신순으로_페이지_조회한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        when(paymentRepository.findByUserId(org.mockito.ArgumentMatchers.eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment)));

        var response = paymentService.getMyPayments(1L, 1, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getMerchantUid()).isEqualTo("payment-1");
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentRepository).findByUserId(org.mockito.ArgumentMatchers.eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }

    @Test
    void 잘못된_결제_내역_페이지_요청은_예외가_발생한다() {
        assertThatThrownBy(() -> paymentService.getMyPayments(1L, 0, 20))
                .isInstanceOf(InvalidPaymentPageRequestException.class)
                .hasMessage("페이지는 1 이상이고, 페이지 크기는 1 이상 100 이하여야 합니다.");
        assertThatThrownBy(() -> paymentService.getMyPayments(1L, 1, 101))
                .isInstanceOf(InvalidPaymentPageRequestException.class);

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void 본인의_결제_상세를_조회한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        ReflectionTestUtils.setField(payment, "paymentId", 7L);
        payment.markPaid("imp-1");
        when(paymentRepository.findById(7L)).thenReturn(Optional.of(payment));

        PaymentDetailResponse response = paymentService.getMyPayment(1L, 7L);

        assertThat(response.getPaymentId()).isEqualTo(7L);
        assertThat(response.getMerchantUid()).isEqualTo("payment-1");
        assertThat(response.getImpUid()).isEqualTo("imp-1");
        assertThat(response.getAmount()).isEqualTo(10000);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void 다른_사용자의_결제_상세는_조회할_수_없다() {
        Payment payment = Payment.createReady(2L, "payment-1", 10000);
        when(paymentRepository.findById(7L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getMyPayment(1L, 7L))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("결제 요청을 찾을 수 없습니다.");
    }

    @Test
    void 결제_확정_성공시_PAID_상태로_변경하고_지갑을_충전한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        ReflectionTestUtils.setField(payment, "paymentId", 7L);
        when(paymentRepository.findWithLockByMerchantUid("payment-1")).thenReturn(Optional.of(payment));
        when(portOneClient.getPayment("payment-1"))
                .thenReturn(new PortOnePaymentResponse("payment-1", 10000, "PAID"));

        PaymentResponse response = paymentService.confirmPaidPayment("payment-1");

        assertThat(response.getMerchantUid()).isEqualTo("payment-1");
        assertThat(response.getImpUid()).isEqualTo("payment-1");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(walletService).chargeByPayment(1L, 10000, 7L);
    }

    @Test
    void 없는_결제_요청은_확정할_수_없다() {
        when(paymentRepository.findWithLockByMerchantUid("payment-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPaidPayment("payment-1"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("결제 요청을 찾을 수 없습니다.");

        verifyNoInteractions(portOneClient);
    }

    @Test
    void 이미_처리된_결제는_다시_충전하지_않고_성공으로_응답한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        payment.markPaid("payment-1");
        when(paymentRepository.findWithLockByMerchantUid("payment-1")).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.confirmPaidPayment("payment-1");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
        verifyNoInteractions(portOneClient);
        verifyNoInteractions(walletService);
    }

    @Test
    void 포트원_결제_요청번호가_다르면_검증에_실패한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        when(paymentRepository.findWithLockByMerchantUid("payment-1")).thenReturn(Optional.of(payment));
        when(portOneClient.getPayment("payment-1"))
                .thenReturn(new PortOnePaymentResponse("payment-2", 10000, "PAID"));

        assertThatThrownBy(() -> paymentService.confirmPaidPayment("payment-1"))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessage("결제 요청 번호가 일치하지 않습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        verifyNoInteractions(walletService);
    }

    @Test
    void 포트원_결제_금액이_다르면_검증에_실패한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        when(paymentRepository.findWithLockByMerchantUid("payment-1")).thenReturn(Optional.of(payment));
        when(portOneClient.getPayment("payment-1"))
                .thenReturn(new PortOnePaymentResponse("payment-1", 9000, "PAID"));

        assertThatThrownBy(() -> paymentService.confirmPaidPayment("payment-1"))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessage("결제 금액이 일치하지 않습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        verifyNoInteractions(walletService);
    }

    @Test
    void 포트원_결제_상태가_PAID가_아니면_검증에_실패한다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        when(paymentRepository.findWithLockByMerchantUid("payment-1")).thenReturn(Optional.of(payment));
        when(portOneClient.getPayment("payment-1"))
                .thenReturn(new PortOnePaymentResponse("payment-1", 10000, "READY"));

        assertThatThrownBy(() -> paymentService.confirmPaidPayment("payment-1"))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessage("결제가 완료되지 않았습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        verifyNoInteractions(walletService);
    }
}
