package com.pacto.api.wallet.controller;

import com.pacto.api.wallet.dto.WithdrawRequest;
import com.pacto.api.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyWallet(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "지갑 조회 성공",
                "data", walletService.getMyWallet(userId)
        ));
    }

    @GetMapping("/me/histories")
    public ResponseEntity<?> getMyHistories(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "포인트 내역 조회 성공",
                "data", walletService.getMyHistories(userId)
        ));
    }

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
