package com.pacto.api.advertiser.controller;

import com.pacto.api.advertiser.dto.AdvertiserDashboardResponse;
import com.pacto.api.advertiser.service.AdvertiserDashboardService;
import com.pacto.api.common.response.CommonResponse;
import com.pacto.api.escrow.dto.EscrowLedgerResponse;
import com.pacto.api.escrow.service.EscrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/advertiser")
@RequiredArgsConstructor
@Tag(name = "Advertiser", description = "광고주 대시보드 API")
public class AdvertiserDashboardController {

    private final AdvertiserDashboardService advertiserDashboardService;
    private final EscrowService escrowService;

    @Operation(summary = "광고주 대시보드 조회")
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        Long advertiserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        AdvertiserDashboardResponse dashboard = advertiserDashboardService.getDashboard(advertiserId);
        return ResponseEntity.ok(CommonResponse.success("대시보드 조회 성공", dashboard));
    }

    @Operation(summary = "광고주 캠페인별 에스크로 목록 조회")
    @GetMapping("/campaigns/{campaignId}/escrows")
    public ResponseEntity<CommonResponse<List<EscrowLedgerResponse>>> getCampaignEscrows(
            Authentication authentication,
            @PathVariable Long campaignId
    ) {
        Long advertiserId = (Long) authentication.getPrincipal();
        List<EscrowLedgerResponse> escrows = escrowService.getAdvertiserCampaignEscrows(advertiserId, campaignId);
        return ResponseEntity.ok(CommonResponse.success("캠페인 에스크로 목록 조회 성공", escrows));
    }
}
