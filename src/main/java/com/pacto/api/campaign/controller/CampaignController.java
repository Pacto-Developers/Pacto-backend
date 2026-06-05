package com.pacto.api.campaign.controller;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.dto.CampaignRequestDto;
import com.pacto.api.campaign.service.CampaignService;
import com.pacto.api.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.pacto.api.mission.domain.Mission;
import com.pacto.api.mission.service.MissionService;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaign", description = "캠페인 등록 및 조회 API")
public class CampaignController {

    private final CampaignService campaignService;
    private final MissionService missionService;

    @Operation(summary = "캠페인 목록 조회")
    @GetMapping
    public ResponseEntity<?> getCampaigns(
            @RequestParam(required = false) CampaignStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Campaign> campaigns = campaignService.getCampaigns(status, pageable);
        return ResponseEntity.ok(CommonResponse.success("캠페인 목록 조회 성공", campaigns));
    }

    @Operation(summary = "캠페인 상세 조회")
    @GetMapping("/{campaignId}")
    public ResponseEntity<?> getCampaign(@PathVariable Long campaignId) {
        Campaign campaign = campaignService.getCampaign(campaignId);
        return ResponseEntity.ok(CommonResponse.success("캠페인 상세 조회 성공", campaign));
    }

    @Operation(summary = "캠페인 등록")
    @PostMapping
    public ResponseEntity<?> createCampaign(@RequestBody CampaignRequestDto dto) {
        Long advertiserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Campaign campaign = campaignService.createCampaign(dto, advertiserId);
        return ResponseEntity.status(201).body(
                CommonResponse.success("캠페인 등록 성공", Map.of(
                        "campaign_id", campaign.getCampaignId(),
                        "status", campaign.getStatus()
                ))
        );
    }

    @Operation(summary = "캠페인 상태 변경")
    @PatchMapping("/{campaignId}/status")
    public ResponseEntity<?> updateCampaignStatus(
            @PathVariable Long campaignId,
            @RequestBody Map<String, String> body) {

        CampaignStatus status = CampaignStatus.valueOf(body.get("status"));
        Campaign campaign = campaignService.updateCampaignStatus(campaignId, status);
        return ResponseEntity.ok(
                CommonResponse.success("캠페인 상태 변경 성공", Map.of(
                        "campaign_id", campaign.getCampaignId(),
                        "status", campaign.getStatus()
                ))
        );
    }

    @Operation(summary = "미션 수락")
    @PostMapping("/{campaignId}/missions")
    public ResponseEntity<?> acceptMission(@PathVariable Long campaignId) {
        Long bloggerId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Mission mission = missionService.acceptMission(campaignId, bloggerId);
        return ResponseEntity.status(201).body(
                CommonResponse.success("미션 수락 성공", Map.of(
                        "mission_id", mission.getMissionId(),
                        "status", mission.getStatus()
                ))
        );
    }
}
