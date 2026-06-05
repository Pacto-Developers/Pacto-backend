package com.pacto.api.application.domain;

import com.pacto.api.common.exception.InvalidApplicationStatusException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Getter
@NoArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;

    @Column(nullable = false)
    private Long campaignId;

    @Column(nullable = false)
    private Long bloggerId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Application(Long campaignId, Long bloggerId) {
        this.campaignId = campaignId;
        this.bloggerId = bloggerId;
        this.status = ApplicationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void accept() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new InvalidApplicationStatusException("대기 중인 지원만 수락할 수 있습니다.");
        }
        this.status = ApplicationStatus.ACCEPTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new InvalidApplicationStatusException("대기 중인 지원만 거절할 수 있습니다.");
        }
        this.status = ApplicationStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new InvalidApplicationStatusException("대기 중인 지원만 취소할 수 있습니다.");
        }
        this.status = ApplicationStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }
}
