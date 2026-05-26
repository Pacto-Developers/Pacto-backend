package com.pacto.api.escrow.controller;

import com.pacto.api.escrow.service.EscrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Escrow", description = "에스크로 잠금 내역 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/escrows")
public class EscrowController {

    private final EscrowService escrowService;

    @Operation(summary = "내 에스크로 잠금 내역 조회", description = "JWT의 userId로 에스크로 잠금 내역 전체를 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getMyEscrows(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "에스크로 내역 조회 성공",
                "data", escrowService.getMyEscrows(userId)
        ));
    }
}
