package com.pacto.api.mission.domain;

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
        if (this.status != MissionStatus.IN_PROGRESS) {
            throw new RuntimeException("진행 중인 미션만 제출할 수 있습니다.");
        }
        this.submittedUrl = submittedUrl;
        this.status = MissionStatus.SUBMITTED;
        this.updatedAt = LocalDateTime.now();
    }

    // 미션 승인
    public void approve() {
        if (this.status != MissionStatus.SUBMITTED) {
            throw new RuntimeException("제출된 미션만 승인할 수 있습니다.");
        }
        this.status = MissionStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
    }

    // 미션 취소
    public void cancel() {
        this.status = MissionStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }
}