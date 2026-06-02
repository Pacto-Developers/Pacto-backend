package com.pacto.api.escrow.controller;

import com.pacto.api.escrow.dto.EscrowLedgerResponse;
import com.pacto.api.escrow.entity.EscrowStatus;
import com.pacto.api.escrow.service.EscrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Escrow", description = "에스크로 잠금 내역 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/escrows")
public class EscrowController {

    private final EscrowService escrowService;

    @Operation(summary = "내 에스크로 잠금 내역 조회", description = "JWT의 userId로 에스크로 잠금 내역 전체를 조회합니다.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = EscrowLedgerResponse.class)))
    @GetMapping
    public ResponseEntity<?> getMyEscrows(
            Authentication authentication,
            @RequestParam(required = false) EscrowStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "에스크로 내역 조회 성공",
                "data", escrowService.getMyEscrows(userId, status, page, size)
        ));
    }
}
