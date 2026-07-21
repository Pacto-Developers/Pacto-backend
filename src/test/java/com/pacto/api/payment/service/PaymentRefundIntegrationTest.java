package com.pacto.api.payment.service;

import com.pacto.api.common.exception.InvalidPaymentRefundException;
import com.pacto.api.common.exception.PortOneApiException;
import com.pacto.api.payment.client.PortOneCancelResponse;
import com.pacto.api.payment.client.PortOneClient;
import com.pacto.api.payment.dto.PaymentRefundResponse;
import com.pacto.api.payment.entity.Payment;
import com.pacto.api.payment.entity.PaymentStatus;
import com.pacto.api.payment.repository.PaymentRefundRepository;
import com.pacto.api.payment.repository.PaymentRepository;
import com.pacto.api.wallet.entity.PointHistoryType;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.repository.PointHistoryRepository;
import com.pacto.api.wallet.repository.WalletRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PaymentRefundIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentRefundRepository paymentRefundRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PortOneClient portOneClient;

    private Payment payment;

    @BeforeEach
    void setUp() {
        pointHistoryRepository.deleteAll();
        paymentRefundRepository.deleteAll();
        paymentRepository.deleteAll();
        walletRepository.deleteAll();

        Wallet wallet = Wallet.create(1L);
        wallet.addBalance(10000);
        walletRepository.save(wallet);

        payment = Payment.createReady(1L, "payment-1", 10000);
        payment.markPaid("payment-1");
        payment = paymentRepository.save(payment);
    }

    @Test
    void 환불_성공시_결제_지갑_환불이력_포인트이력을_함께_반영한다() {
        when(portOneClient.cancelPayment(
                "payment-1", 3000, 10000, "부분 환불", "refund-integration-1"
        )).thenReturn(new PortOneCancelResponse("cancellation-1", 3000, "SUCCEEDED"));

        PaymentRefundResponse response = paymentService.refundPayment(
                1L,
                payment.getPaymentId(),
                3000,
                "부분 환불",
                "refund-integration-1"
        );

        entityManager.clear();
        Payment savedPayment = paymentRepository.findById(payment.getPaymentId()).orElseThrow();
        Wallet savedWallet = walletRepository.findByUserId(1L).orElseThrow();

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(savedPayment.getRefundedAmount()).isEqualTo(3000);
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(savedWallet.getBalance()).isEqualTo(7000);
        assertThat(paymentRefundRepository.count()).isEqualTo(1);
        assertThat(pointHistoryRepository.findAll())
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.getAmount()).isEqualTo(-3000);
                    assertThat(history.getType()).isEqualTo(PointHistoryType.PAYMENT_REFUND);
                    assertThat(history.getReferenceId()).isEqualTo(response.getRefundId());
                });
    }

    @Test
    void 포트원_취소가_실패하면_모든_로컬_변경을_롤백한다() {
        when(portOneClient.cancelPayment(
                "payment-1", 3000, 10000, "부분 환불", "refund-integration-2"
        )).thenThrow(new PortOneApiException("포트원 결제 취소에 실패했습니다."));

        assertThatThrownBy(() -> paymentService.refundPayment(
                1L,
                payment.getPaymentId(),
                3000,
                "부분 환불",
                "refund-integration-2"
        ))
                .isInstanceOf(PortOneApiException.class)
                .hasMessage("포트원 결제 취소에 실패했습니다.");

        entityManager.clear();
        Payment savedPayment = paymentRepository.findById(payment.getPaymentId()).orElseThrow();
        Wallet savedWallet = walletRepository.findByUserId(1L).orElseThrow();

        assertThat(savedPayment.getRefundedAmount()).isZero();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedWallet.getBalance()).isEqualTo(10000);
        assertThat(paymentRefundRepository.count()).isZero();
        assertThat(pointHistoryRepository.count()).isZero();
    }

    @Test
    void 같은_멱등성_키를_재요청해도_한_번만_환불한다() {
        when(portOneClient.cancelPayment(
                "payment-1", 3000, 10000, "부분 환불", "refund-integration-3"
        )).thenReturn(new PortOneCancelResponse("cancellation-1", 3000, "SUCCEEDED"));

        PaymentRefundResponse first = paymentService.refundPayment(
                1L, payment.getPaymentId(), 3000, "부분 환불", "refund-integration-3"
        );
        PaymentRefundResponse second = paymentService.refundPayment(
                1L, payment.getPaymentId(), 3000, "부분 환불", "refund-integration-3"
        );

        entityManager.clear();
        Wallet savedWallet = walletRepository.findByUserId(1L).orElseThrow();

        assertThat(second.getRefundId()).isEqualTo(first.getRefundId());
        assertThat(savedWallet.getBalance()).isEqualTo(7000);
        assertThat(paymentRefundRepository.count()).isEqualTo(1);
        assertThat(pointHistoryRepository.count()).isEqualTo(1);
        verify(portOneClient, times(1)).cancelPayment(
                "payment-1", 3000, 10000, "부분 환불", "refund-integration-3"
        );
    }

    @Test
    void 동시에_남은_금액보다_큰_두_환불이_들어오면_한_건만_성공한다() throws Exception {
        when(portOneClient.cancelPayment(
                eq("payment-1"),
                eq(7000),
                eq(10000),
                eq("동시 환불"),
                anyString()
        )).thenReturn(new PortOneCancelResponse("cancellation-1", 7000, "SUCCEEDED"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Object> first = executor.submit(() -> refundConcurrently(
                    "refund-concurrent-1", ready, start
            ));
            Future<Object> second = executor.submit(() -> refundConcurrently(
                    "refund-concurrent-2", ready, start
            ));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Object> results = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );

            assertThat(results.stream().filter(PaymentRefundResponse.class::isInstance).count())
                    .isEqualTo(1);
            assertThat(results.stream().filter(InvalidPaymentRefundException.class::isInstance).count())
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }

        entityManager.clear();
        Payment savedPayment = paymentRepository.findById(payment.getPaymentId()).orElseThrow();
        Wallet savedWallet = walletRepository.findByUserId(1L).orElseThrow();

        assertThat(savedPayment.getRefundedAmount()).isEqualTo(7000);
        assertThat(savedWallet.getBalance()).isEqualTo(3000);
        assertThat(paymentRefundRepository.count()).isEqualTo(1);
        assertThat(pointHistoryRepository.count()).isEqualTo(1);
        verify(portOneClient, times(1)).cancelPayment(
                eq("payment-1"),
                eq(7000),
                eq(10000),
                eq("동시 환불"),
                anyString()
        );
    }

    private Object refundConcurrently(
            String idempotencyKey,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        try {
            start.await();
            return paymentService.refundPayment(
                    1L,
                    payment.getPaymentId(),
                    7000,
                    "동시 환불",
                    idempotencyKey
            );
        } catch (Exception e) {
            return e;
        }
    }
}
