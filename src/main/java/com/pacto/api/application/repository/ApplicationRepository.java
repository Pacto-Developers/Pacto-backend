package com.pacto.api.application.repository;

import com.pacto.api.application.domain.Application;
import com.pacto.api.application.domain.ApplicationStatus;
import com.pacto.api.application.dto.ApplicationResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    @Query("SELECT new com.pacto.api.application.dto.ApplicationResponse(" +
            "a.applicationId, a.campaignId, a.bloggerId, u.email, a.status, a.createdAt, a.updatedAt) " +
            "FROM Application a JOIN User u ON a.bloggerId = u.userId " +
            "WHERE a.campaignId = :campaignId")
    List<ApplicationResponse> findByCampaignIdWithBloggerEmail(@Param("campaignId") Long campaignId);

    @Query("SELECT new com.pacto.api.application.dto.ApplicationResponse(" +
            "a.applicationId, a.campaignId, a.bloggerId, u.email, a.status, a.createdAt, a.updatedAt) " +
            "FROM Application a JOIN User u ON a.bloggerId = u.userId " +
            "WHERE a.campaignId = :campaignId AND a.status = :status")
    List<ApplicationResponse> findByCampaignIdAndStatusWithBloggerEmail(@Param("campaignId") Long campaignId,
                                                                         @Param("status") ApplicationStatus status);

    List<Application> findByCampaignId(Long campaignId);
    List<Application> findByBloggerId(Long bloggerId);
    List<Application> findByCampaignIdAndStatus(Long campaignId, ApplicationStatus status);
}
