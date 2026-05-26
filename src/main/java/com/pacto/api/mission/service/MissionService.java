package com.pacto.api.mission.service;

import com.pacto.api.mission.domain.Mission;
import com.pacto.api.mission.domain.MissionStatus;
import com.pacto.api.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;

    // 내 미션 목록 조회
    @Transactional(readOnly = true)
    public List<Mission> getMyMissions(Long bloggerId, MissionStatus status) {
        if (status != null) {
            return missionRepository.findByBloggerIdAndStatus(bloggerId, status);
        }
        return missionRepository.findByBloggerId(bloggerId);
    }

    // URL 제출
    @Transactional
    public Mission submitMission(Long missionId, String submittedUrl) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("미션을 찾을 수 없습니다."));
        mission.submit(submittedUrl);
        return missionRepository.save(mission);
    }

    // 미션 승인
    @Transactional
    public Mission approveMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("미션을 찾을 수 없습니다."));
        mission.approve();
        return missionRepository.save(mission);
    }

    // 미션 취소
    @Transactional
    public Mission cancelMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("미션을 찾을 수 없습니다."));
        mission.cancel();
        return missionRepository.save(mission);
    }
}