package com.pacto.api.campaign.controller;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.dto.CampaignRequestDto;
import com.pacto.api.campaign.dto.CampaignResponseDto;
import com.pacto.api.campaign.service.CampaignService;
import com.pacto.api.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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

        Page<CampaignResponseDto> campaigns = campaignService.getCampaigns(status, pageable)
                .map(campaignService::toResponseDto);
        return ResponseEntity.ok(CommonResponse.success("캠페인 목록 조회 성공", campaigns));
    }

    @Operation(summary = "캠페인 상세 조회")
    @GetMapping("/{campaignId}")
    public ResponseEntity<?> getCampaign(@PathVariable Long campaignId) {
        Campaign campaign = campaignService.getCampaign(campaignId);
        return ResponseEntity.ok(CommonResponse.success("캠페인 상세 조회 성공", campaignService.toResponseDto(campaign)));
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

    @Operation(summary = "캠페인 썸네일 업로드")
    @PostMapping(value = "/{campaignId}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadThumbnail(@PathVariable Long campaignId,
                                              @RequestParam("file") MultipartFile file) {
        Long advertiserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Campaign campaign = campaignService.uploadThumbnail(campaignId, advertiserId, file);
        return ResponseEntity.ok(CommonResponse.success("캠페인 썸네일 업로드 성공", Map.of(
                "campaign_id", campaign.getCampaignId(),
                "thumbnail_key", campaign.getThumbnailUrl()
        )));
    }

    @Operation(summary = "캠페인 가이드라인 이미지 업로드")
    @PostMapping(value = "/{campaignId}/guideline-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadGuidelineImages(@PathVariable Long campaignId,
                                                    @RequestParam("files") List<MultipartFile> files) {
        Long advertiserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Campaign campaign = campaignService.uploadGuidelineImages(campaignId, advertiserId, files);
        return ResponseEntity.ok(CommonResponse.success("캠페인 가이드라인 이미지 업로드 성공", Map.of(
                "campaign_id", campaign.getCampaignId(),
                "guideline_image_keys", campaign.getGuidelineImageKeys()
        )));
    }

    @Operation(summary = "캠페인 모집 마감 (RECRUITING → CLOSED)")
    @PatchMapping("/{campaignId}/close")
    public ResponseEntity<?> closeCampaign(@PathVariable Long campaignId) {
        Long advertiserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Campaign campaign = campaignService.closeCampaign(campaignId, advertiserId);
        return ResponseEntity.ok(CommonResponse.success("캠페인 모집 마감 성공", Map.of(
                "campaign_id", campaign.getCampaignId(),
                "status", campaign.getStatus()
        )));
    }

    @Operation(summary = "캠페인 진행 전환 (CLOSED → IN_PROGRESS)")
    @PatchMapping("/{campaignId}/proceed")
    public ResponseEntity<?> proceedCampaign(@PathVariable Long campaignId) {
        Long advertiserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Campaign campaign = campaignService.proceedCampaign(campaignId, advertiserId);
        return ResponseEntity.ok(CommonResponse.success("캠페인 진행 전환 성공", Map.of(
                "campaign_id", campaign.getCampaignId(),
                "status", campaign.getStatus()
        )));
    }

    @Operation(summary = "캠페인 취소 (RECRUITING 또는 CLOSED → CANCELLED)")
    @PatchMapping("/{campaignId}/cancel")
    public ResponseEntity<?> cancelCampaign(@PathVariable Long campaignId) {
        Long advertiserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Campaign campaign = campaignService.cancelCampaign(campaignId, advertiserId);
        return ResponseEntity.ok(CommonResponse.success("캠페인 취소 성공", Map.of(
                "campaign_id", campaign.getCampaignId(),
                "status", campaign.getStatus()
        )));
    }

    // 캠페인 미션 목록 조회
    @Operation(summary = "캠페인 미션 목록 조회")
    @GetMapping("/{campaignId}/missions")
    public ResponseEntity<?> getCampaignMissions(@PathVariable Long campaignId) {
        Long advertiserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        List<Mission> missions = missionService.getMissionsByCampaignId(campaignId, advertiserId);
        return ResponseEntity.ok(
                CommonResponse.success("캠페인 미션 목록 조회 성공", missions)
        );
    }
}
