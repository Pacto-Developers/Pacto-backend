package com.pacto.api.mission.controller;

import com.pacto.api.common.response.CommonResponse;
import com.pacto.api.mission.domain.Mission;
import com.pacto.api.mission.domain.MissionStatus;
import com.pacto.api.mission.dto.MissionRequestDto;
import com.pacto.api.mission.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
@Tag(name = "Mission", description = "미션 수락 및 관리 API")
public class MissionController {

    private final MissionService missionService;

    @Operation(summary = "내 미션 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<?> getMyMissions(
            @RequestParam(required = false) MissionStatus status) {
        Long bloggerId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        List<Mission> missions = missionService.getMyMissions(bloggerId, status);
        return ResponseEntity.ok(
                CommonResponse.success("미션 목록 조회 성공", missions)
        );
    }

    @Operation(summary = "URL 제출")
    @PatchMapping("/{missionId}/submit")
    public ResponseEntity<?> submitMission(
            @PathVariable Long missionId,
            @RequestBody MissionRequestDto dto) {
        Mission mission = missionService.submitMission(missionId, dto.getSubmittedUrl());
        return ResponseEntity.ok(
                CommonResponse.success("미션 제출 성공", Map.of(
                        "mission_id", mission.getMissionId(),
                        "status", mission.getStatus()
                ))
        );
    }

    @Operation(summary = "미션 승인")
    @PatchMapping("/{missionId}/approve")
    public ResponseEntity<?> approveMission(@PathVariable Long missionId) {
        Mission mission = missionService.approveMission(missionId);
        return ResponseEntity.ok(
                CommonResponse.success("미션 승인 완료", Map.of(
                        "mission_id", mission.getMissionId(),
                        "status", mission.getStatus()
                ))
        );
    }

    @Operation(summary = "미션 취소")
    @PatchMapping("/{missionId}/cancel")
    public ResponseEntity<?> cancelMission(
            @PathVariable Long missionId,
            @RequestBody MissionRequestDto dto) {
        Mission mission = missionService.cancelMission(missionId);
        return ResponseEntity.ok(
                CommonResponse.success("미션 취소 완료", Map.of(
                        "mission_id", mission.getMissionId(),
                        "status", mission.getStatus()
                ))
        );
    }
}
