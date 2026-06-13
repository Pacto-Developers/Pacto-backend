package com.pacto.api.advertiser.controller;

import com.pacto.api.advertiser.dto.AdvertiserDashboardResponse;
import com.pacto.api.advertiser.service.AdvertiserDashboardService;
import com.pacto.api.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/advertiser")
@RequiredArgsConstructor
@Tag(name = "Advertiser", description = "광고주 대시보드 API")
public class AdvertiserDashboardController {

    private final AdvertiserDashboardService advertiserDashboardService;

    @Operation(summary = "광고주 대시보드 조회")
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        Long advertiserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        AdvertiserDashboardResponse dashboard = advertiserDashboardService.getDashboard(advertiserId);
        return ResponseEntity.ok(CommonResponse.success("대시보드 조회 성공", dashboard));
    }
}
