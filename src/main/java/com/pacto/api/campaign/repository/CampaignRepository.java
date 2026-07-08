package com.pacto.api.campaign.repository;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    Page<Campaign> findByStatus(CampaignStatus status, Pageable pageable);
    Page<Campaign> findByStatusNotIn(List<CampaignStatus> statuses, Pageable pageable);
    List<Campaign> findByAdvertiserId(Long advertiserId);
    long countByAdvertiserIdAndStatus(Long advertiserId, CampaignStatus status);
    List<Campaign> findByDeadlineBeforeAndStatus(LocalDateTime deadline, CampaignStatus status);
}