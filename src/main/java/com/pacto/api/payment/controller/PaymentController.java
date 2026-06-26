package com.pacto.api.payment.controller;

import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.response.CommonResponse;
import com.pacto.api.payment.dto.PaymentCreateRequest;
import com.pacto.api.payment.dto.PaymentDetailResponse;
import com.pacto.api.payment.dto.PaymentResponse;
import com.pacto.api.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

}
