package com.pacto.api.escrow.controller;

import com.pacto.api.escrow.service.EscrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/escrows")
public class EscrowController {

    private final EscrowService escrowService;

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
