package com.pacto.api.mission.domain;

import com.pacto.api.common.exception.InvalidMissionStatusException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "missions")
@Getter
@NoArgsConstructor
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long missionId;

    @Column(nullable = false)
    private Long campaignId;

    @Column(nullable = false)
    private Long bloggerId;

    @Column(nullable = false, unique = true)
    private Long escrowId;

    private String submittedUrl;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MissionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // URL 제출
    public void submit(String submittedUrl) {
        if (this.status != MissionStatus.READY) {
            throw new InvalidMissionStatusException("대기 중인 미션만 제출할 수 있습니다.");
        }
        this.submittedUrl = submittedUrl;
        this.status = MissionStatus.SUBMITTED;
        this.updatedAt = LocalDateTime.now();
    }

    // 미션 승인
    public void approve() {
        if (this.status != MissionStatus.SUBMITTED) {
            throw new InvalidMissionStatusException("제출된 미션만 승인할 수 있습니다.");
        }
        this.status = MissionStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
    }

    // 미션 반려 (광고주가 제출된 URL 반려)
    public void reject() {
        if (this.status != MissionStatus.SUBMITTED) {
            throw new InvalidMissionStatusException("제출된 미션만 반려할 수 있습니다.");
        }
        this.status = MissionStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    // 미션 취소
    public void cancel() {
        if (this.status == MissionStatus.APPROVED) {
            throw new InvalidMissionStatusException("이미 승인된 미션은 취소할 수 없습니다.");
        }
        if (this.status == MissionStatus.CANCELLED) {
            throw new InvalidMissionStatusException("이미 취소된 미션입니다.");
        }
        this.status = MissionStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public Mission(Long campaignId, Long bloggerId) {
        this.campaignId = campaignId;
        this.bloggerId = bloggerId;
        this.status = MissionStatus.READY;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Mission(Long campaignId, Long bloggerId, Long escrowId) {
        this.campaignId = campaignId;
        this.bloggerId = bloggerId;
        this.escrowId = escrowId;
        this.status = MissionStatus.READY;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}