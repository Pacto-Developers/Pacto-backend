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
}