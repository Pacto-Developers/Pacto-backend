package com.pacto.api.campaign.controller;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.dto.CampaignRequestDto;
import com.pacto.api.campaign.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.pacto.api.mission.domain.Mission;
import com.pacto.api.mission.service.MissionService;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final MissionService missionService;

    // 캠페인 목록 조회
    @GetMapping
    public ResponseEntity<?> getCampaigns(
            @RequestParam(required = false) CampaignStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Campaign> campaigns = campaignService.getCampaigns(status, pageable);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "캠페인 목록 조회 성공",
                "data", campaigns
        ));
    }

    // 캠페인 상세 조회
    @GetMapping("/{campaignId}")
    public ResponseEntity<?> getCampaign(@PathVariable Long campaignId) {
        Campaign campaign = campaignService.getCampaign(campaignId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "캠페인 상세 조회 성공",
                "data", campaign
        ));
    }

    // 캠페인 등록
    @PostMapping
    public ResponseEntity<?> createCampaign(@RequestBody CampaignRequestDto dto) {
        Campaign campaign = campaignService.createCampaign(dto);
        return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "캠페인 등록 성공",
                "data", Map.of(
                        "campaign_id", campaign.getCampaignId(),
                        "status", campaign.getStatus()
                )
        ));
    }

    // 캠페인 상태 변경
    @PatchMapping("/{campaignId}/status")
    public ResponseEntity<?> updateCampaignStatus(
            @PathVariable Long campaignId,
            @RequestBody Map<String, String> body) {

        CampaignStatus status = CampaignStatus.valueOf(body.get("status"));
        Campaign campaign = campaignService.updateCampaignStatus(campaignId, status);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "캠페인 상태 변경 성공",
                "data", Map.of(
                        "campaign_id", campaign.getCampaignId(),
                        "status", campaign.getStatus()
                )
        ));
    }

    // 미션 수락
    @PostMapping("/{campaignId}/missions")
    public ResponseEntity<?> acceptMission(
            @PathVariable Long campaignId,
            @RequestParam Long bloggerId) {
        Mission mission = missionService.acceptMission(campaignId, bloggerId);
        return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "미션 수락 성공",
                "data", Map.of(
                        "mission_id", mission.getMissionId(),
                        "status", mission.getStatus()
                )
        ));
    }
}