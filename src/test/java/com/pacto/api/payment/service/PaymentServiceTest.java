package com.pacto.api.payment.service;

import com.pacto.api.common.exception.InsufficientBalanceException;
import com.pacto.api.common.exception.InvalidPaymentRefundException;
import com.pacto.api.common.exception.InvalidPaymentPageRequestException;
import com.pacto.api.common.exception.PaymentNotFoundException;
import com.pacto.api.common.exception.PaymentRefundNotAllowedException;
import com.pacto.api.common.exception.PaymentVerificationException;
import com.pacto.api.common.exception.PortOneApiException;
import com.pacto.api.payment.client.PortOneCancelResponse;
import com.pacto.api.payment.client.PortOneClient;
import com.pacto.api.payment.client.PortOnePaymentResponse;
import com.pacto.api.payment.dto.PaymentCreateRequest;
import com.pacto.api.payment.dto.PaymentDetailResponse;
import com.pacto.api.payment.dto.PaymentResponse;
import com.pacto.api.payment.entity.Payment;
import com.pacto.api.payment.entity.PaymentRefund;
import com.pacto.api.payment.entity.PaymentRefundStatus;
import com.pacto.api.payment.entity.PaymentStatus;
import com.pacto.api.payment.repository.PaymentRefundRepository;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentRefundRepository paymentRefundRepository;
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

    @Test
    void 결제_금액의_일부를_환불한다() {
        Payment payment = paidPayment(10000);
        stubRefund(payment, 15L);
        when(portOneClient.cancelPayment(
                "payment-1", 3000, 10000, "부분 환불", "refund-request-1"
        )).thenReturn(new PortOneCancelResponse("cancellation-1", 3000, "SUCCEEDED"));

        PaymentRefund refund = paymentService.refundPayment(
                1L, 7L, 3000, "부분 환불", "refund-request-1"
        );

        assertThat(refund.getRefundId()).isEqualTo(15L);
        assertThat(refund.getStatus()).isEqualTo(PaymentRefundStatus.SUCCEEDED);
        assertThat(refund.getPortoneCancellationId()).isEqualTo("cancellation-1");
        assertThat(payment.getRefundedAmount()).isEqualTo(3000);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        verify(walletService).deductByPaymentRefund(1L, 3000, 15L);
    }

    @Test
    void 남은_결제_금액_전체를_환불하면_REFUNDED_상태가_된다() {
        Payment payment = paidPayment(10000);
        payment.applyRefund(3000);
        stubRefund(payment, 16L);
        when(portOneClient.cancelPayment(
                "payment-1", 7000, 7000, "나머지 환불", "refund-request-2"
        )).thenReturn(new PortOneCancelResponse("cancellation-2", 7000, "SUCCEEDED"));

        PaymentRefund refund = paymentService.refundPayment(
                1L, 7L, 7000, "나머지 환불", "refund-request-2"
        );

        assertThat(refund.getStatus()).isEqualTo(PaymentRefundStatus.SUCCEEDED);
        assertThat(payment.getRefundedAmount()).isEqualTo(10000);
        assertThat(payment.getRefundableAmount()).isZero();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void 같은_멱등성_키로_재요청하면_기존_환불을_반환한다() {
        Payment payment = paidPayment(10000);
        PaymentRefund existingRefund = PaymentRefund.create(
                payment, 3000, "부분 환불", "refund-request-1"
        );
        existingRefund.markSucceeded("cancellation-1");
        when(paymentRepository.findWithLockByPaymentId(7L)).thenReturn(Optional.of(payment));
        when(paymentRefundRepository.findByPayment_PaymentIdAndIdempotencyKey(
                7L, "refund-request-1"
        )).thenReturn(Optional.of(existingRefund));

        PaymentRefund refund = paymentService.refundPayment(
                1L, 7L, 3000, "부분 환불", "refund-request-1"
        );

        assertThat(refund).isSameAs(existingRefund);
        verifyNoInteractions(portOneClient, walletService);
    }

    @Test
    void 다른_사용자의_결제는_환불할_수_없다() {
        Payment payment = paidPayment(10000);
        when(paymentRepository.findWithLockByPaymentId(7L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refundPayment(
                2L, 7L, 3000, "부분 환불", "refund-request-1"
        ))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("결제 요청을 찾을 수 없습니다.");

        verifyNoInteractions(paymentRefundRepository, portOneClient, walletService);
    }

    @Test
    void PAID나_PARTIALLY_REFUNDED가_아니면_환불할_수_없다() {
        Payment payment = Payment.createReady(1L, "payment-1", 10000);
        ReflectionTestUtils.setField(payment, "paymentId", 7L);
        when(paymentRepository.findWithLockByPaymentId(7L)).thenReturn(Optional.of(payment));
        when(paymentRefundRepository.findByPayment_PaymentIdAndIdempotencyKey(
                7L, "refund-request-1"
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refundPayment(
                1L, 7L, 3000, "부분 환불", "refund-request-1"
        ))
                .isInstanceOf(PaymentRefundNotAllowedException.class)
                .hasMessage("환불할 수 없는 결제 상태입니다.");

        verifyNoInteractions(portOneClient, walletService);
    }

    @Test
    void 남은_환불_가능_금액을_초과하면_환불할_수_없다() {
        Payment payment = paidPayment(10000);
        payment.applyRefund(3000);
        when(paymentRepository.findWithLockByPaymentId(7L)).thenReturn(Optional.of(payment));
        when(paymentRefundRepository.findByPayment_PaymentIdAndIdempotencyKey(
                7L, "refund-request-1"
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refundPayment(
                1L, 7L, 7001, "부분 환불", "refund-request-1"
        ))
                .isInstanceOf(InvalidPaymentRefundException.class)
                .hasMessage("남은 환불 가능 금액을 초과했습니다.");

        verifyNoInteractions(portOneClient, walletService);
    }

    @Test
    void 지갑_가용_잔액이_부족하면_포트원_취소를_호출하지_않는다() {
        Payment payment = paidPayment(10000);
        stubRefund(payment, 15L);
        doThrow(new InsufficientBalanceException())
                .when(walletService).deductByPaymentRefund(1L, 3000, 15L);

        assertThatThrownBy(() -> paymentService.refundPayment(
                1L, 7L, 3000, "부분 환불", "refund-request-1"
        ))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("잔액이 부족합니다.");

        verifyNoInteractions(portOneClient);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void 포트원_취소_금액이_다르면_로컬_결제_상태를_변경하지_않는다() {
        Payment payment = paidPayment(10000);
        stubRefund(payment, 15L);
        when(portOneClient.cancelPayment(
                "payment-1", 3000, 10000, "부분 환불", "refund-request-1"
        )).thenReturn(new PortOneCancelResponse("cancellation-1", 2000, "SUCCEEDED"));

        assertThatThrownBy(() -> paymentService.refundPayment(
                1L, 7L, 3000, "부분 환불", "refund-request-1"
        ))
                .isInstanceOf(PortOneApiException.class)
                .hasMessage("포트원 결제 취소 금액이 일치하지 않습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getRefundedAmount()).isZero();
    }

    @Test
    void 포트원_취소가_완료되지_않으면_로컬_결제_상태를_변경하지_않는다() {
        Payment payment = paidPayment(10000);
        stubRefund(payment, 15L);
        when(portOneClient.cancelPayment(
                "payment-1", 3000, 10000, "부분 환불", "refund-request-1"
        )).thenReturn(new PortOneCancelResponse("cancellation-1", 3000, "REQUESTED"));

        assertThatThrownBy(() -> paymentService.refundPayment(
                1L, 7L, 3000, "부분 환불", "refund-request-1"
        ))
                .isInstanceOf(PortOneApiException.class)
                .hasMessage("포트원 결제 취소가 완료되지 않았습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getRefundedAmount()).isZero();
    }

    @Test
    void 잘못된_환불_요청은_DB를_조회하지_않는다() {
        assertThatThrownBy(() -> paymentService.refundPayment(
                1L, 7L, 0, "부분 환불", "refund-request-1"
        ))
                .isInstanceOf(InvalidPaymentRefundException.class)
                .hasMessage("환불 금액은 0보다 커야 합니다.");

        verifyNoInteractions(paymentRepository, paymentRefundRepository, portOneClient, walletService);
    }

    private Payment paidPayment(int amount) {
        Payment payment = Payment.createReady(1L, "payment-1", amount);
        ReflectionTestUtils.setField(payment, "paymentId", 7L);
        payment.markPaid("payment-1");
        return payment;
    }

    private void stubRefund(Payment payment, Long refundId) {
        when(paymentRepository.findWithLockByPaymentId(7L)).thenReturn(Optional.of(payment));
        when(paymentRefundRepository.findByPayment_PaymentIdAndIdempotencyKey(
                7L, "refund-request-" + (refundId.equals(16L) ? "2" : "1")
        )).thenReturn(Optional.empty());
        when(paymentRefundRepository.save(any(PaymentRefund.class))).thenAnswer(invocation -> {
            PaymentRefund refund = invocation.getArgument(0);
            ReflectionTestUtils.setField(refund, "refundId", refundId);
            return refund;
        });
    }
}
