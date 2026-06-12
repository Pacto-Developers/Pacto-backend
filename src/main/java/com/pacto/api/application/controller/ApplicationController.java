package com.pacto.api.application.controller;

import com.pacto.api.application.domain.Application;
import com.pacto.api.application.domain.ApplicationStatus;
import com.pacto.api.application.dto.ApplicationRequest;
import com.pacto.api.application.dto.ApplicationResponse;
import com.pacto.api.application.service.ApplicationService;
import com.pacto.api.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Application", description = "캠페인 지원 API")
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "캠페인 지원 (블로거)")
    @PostMapping
    public ResponseEntity<?> apply(@RequestBody ApplicationRequest dto) {
        Long bloggerId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Application application = applicationService.apply(dto.getCampaignId(), bloggerId);
        return ResponseEntity.ok(CommonResponse.success("지원 완료", Map.of(
                "application_id", application.getApplicationId(),
                "status", application.getStatus()
        )));
    }

    @Operation(summary = "지원 수락 (광고주) - 에스크로 LOCK + 미션 생성")
    @PatchMapping("/{applicationId}/accept")
    public ResponseEntity<?> accept(@PathVariable Long applicationId) {
        Application application = applicationService.acceptApplication(applicationId);
        return ResponseEntity.ok(CommonResponse.success("지원 수락 완료", Map.of(
                "application_id", application.getApplicationId(),
                "status", application.getStatus()
        )));
    }

    @Operation(summary = "지원 거절 (광고주)")
    @PatchMapping("/{applicationId}/reject")
    public ResponseEntity<?> reject(@PathVariable Long applicationId) {
        Application application = applicationService.rejectApplication(applicationId);
        return ResponseEntity.ok(CommonResponse.success("지원 거절 완료", Map.of(
                "application_id", application.getApplicationId(),
                "status", application.getStatus()
        )));
    }

    @Operation(summary = "지원 취소 (블로거)")
    @PatchMapping("/{applicationId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long applicationId) {
        Long bloggerId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Application application = applicationService.cancelApplication(applicationId, bloggerId);
        return ResponseEntity.ok(CommonResponse.success("지원 취소 완료", Map.of(
                "application_id", application.getApplicationId(),
                "status", application.getStatus()
        )));
    }

    @Operation(summary = "캠페인별 지원 목록 조회 (광고주)")
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<?> getByCampaign(@PathVariable Long campaignId,
                                           @RequestParam(required = false) ApplicationStatus status) {
        List<ApplicationResponse> applications = applicationService.getApplicationsByCampaign(campaignId, status);
        return ResponseEntity.ok(CommonResponse.success("지원 목록 조회 성공", applications));
    }

    @Operation(summary = "내 지원 목록 조회 (블로거)")
    @GetMapping("/me")
    public ResponseEntity<?> getMyApplications(@RequestParam(required = false) ApplicationStatus status) {
        Long bloggerId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        List<Application> applications = applicationService.getMyApplications(bloggerId, status);
        return ResponseEntity.ok(CommonResponse.success("내 지원 목록 조회 성공", applications));
    }
}
