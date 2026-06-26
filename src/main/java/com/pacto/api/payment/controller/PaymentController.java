package com.pacto.api.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacto.api.common.exception.PaymentVerificationException;
import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.response.CommonResponse;
import com.pacto.api.payment.dto.PaymentCreateRequest;
import com.pacto.api.payment.dto.PaymentDetailResponse;
import com.pacto.api.payment.dto.PaymentResponse;
import com.pacto.api.payment.dto.PaymentWebhookRequest;
import com.pacto.api.payment.service.PaymentService;
import com.pacto.api.payment.service.PortOneWebhookVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment", description = "포트원 결제 요청 및 검증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PortOneWebhookVerifier portOneWebhookVerifier;
    private final ObjectMapper objectMapper;

    @Operation(summary = "내 결제 내역 조회", description = "JWT의 userId로 결제 내역을 최신순으로 조회합니다.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<PaymentResponse>>> getMyPayments(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(
                CommonResponse.success("결제 내역 조회 성공", paymentService.getMyPayments(userId, page, size))
        );
    }

    @Operation(summary = "내 결제 상세 조회", description = "JWT의 userId와 일치하는 결제 내역만 조회합니다.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PaymentDetailResponse.class)))
    @GetMapping("/{paymentId}")
    public ResponseEntity<CommonResponse<PaymentDetailResponse>> getMyPayment(
            Authentication authentication,
            @PathVariable Long paymentId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(
                CommonResponse.success("결제 상세 조회 성공", paymentService.getMyPayment(userId, paymentId))
        );
    }

    @Operation(summary = "결제 요청 생성", description = "JWT의 userId로 결제 요청을 생성하고 merchantUid를 발급합니다.")
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    @PostMapping
    public ResponseEntity<CommonResponse<PaymentResponse>> createPayment(
            Authentication authentication,
            @RequestBody PaymentCreateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success("결제 요청 생성 성공", paymentService.createPayment(userId, request)));
    }

    @Operation(summary = "포트원 결제 웹훅 수신", description = "포트원 V2 결제 완료 웹훅을 수신해 결제를 확정합니다.")
    @ApiResponse(responseCode = "200")
    @PostMapping("/webhook/portone")
    public ResponseEntity<Void> handlePortOneWebhook(
            @RequestBody String payload,
            @RequestHeader HttpHeaders headers
    ) {
        portOneWebhookVerifier.verify(payload, headers);

        PaymentWebhookRequest request = parseWebhookRequest(payload);
        if (request.isPaidTransaction() && request.paymentId() != null) {
            paymentService.confirmPaidPayment(request.paymentId());
        }

        return ResponseEntity.ok().build();
    }

    private PaymentWebhookRequest parseWebhookRequest(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentWebhookRequest.class);
        } catch (JsonProcessingException e) {
            throw new PaymentVerificationException("포트원 웹훅 요청 형식이 올바르지 않습니다.");
        }
    }

}
