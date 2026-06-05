package com.pacto.api.application.dto;

import com.pacto.api.application.domain.ApplicationStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApplicationResponse {

    private final Long applicationId;
    private final Long campaignId;
    private final Long bloggerId;
    private final String bloggerEmail;
    private final ApplicationStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ApplicationResponse(Long applicationId, Long campaignId, Long bloggerId,
                               String bloggerEmail, ApplicationStatus status,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.applicationId = applicationId;
        this.campaignId = campaignId;
        this.bloggerId = bloggerId;
        this.bloggerEmail = bloggerEmail;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
