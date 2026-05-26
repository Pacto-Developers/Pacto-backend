package com.pacto.api.wallet.controller;

import com.pacto.api.wallet.dto.WithdrawRequest;
import com.pacto.api.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Wallet", description = "지갑 잔액 및 출금 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "내 지갑 잔액 조회", description = "JWT의 userId로 지갑 잔액 및 잠금 잔액을 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<?> getMyWallet(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "지갑 조회 성공",
                "data", walletService.getMyWallet(userId)
        ));
    }

    @Operation(summary = "포인트 변동 내역 조회", description = "JWT의 userId로 포인트 변동 내역 전체를 조회합니다.")
    @GetMapping("/me/histories")
    public ResponseEntity<?> getMyHistories(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "포인트 내역 조회 성공",
                "data", walletService.getMyHistories(userId)
        ));
    }

    @Operation(summary = "출금 신청", description = "잔액을 검증하고 출금 신청(PENDING)을 생성합니다. 잔액이 부족하면 400을 반환합니다.")
    @PostMapping("/withdraw")
    public ResponseEntity<?> requestWithdraw(
            Authentication authentication,
            @RequestBody WithdrawRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "출금 신청 성공",
                "data", walletService.requestWithdraw(userId, request)
        ));
    }
}
