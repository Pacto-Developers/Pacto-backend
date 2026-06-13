package com.pacto.api.mission.repository;

import com.pacto.api.mission.domain.Mission;
import com.pacto.api.mission.domain.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    List<Mission> findByBloggerIdAndStatus(Long bloggerId, MissionStatus status);
    List<Mission> findByBloggerId(Long bloggerId);
    List<Mission> findByCampaignId(Long campaignId);
    List<Mission> findByCampaignIdInAndStatus(List<Long> campaignIds, MissionStatus status);
    long countByCampaignIdInAndStatus(List<Long> campaignIds, MissionStatus status);
}