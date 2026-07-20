package com.pacto.api.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = @Index(name = "idx_notifications_user_created", columnList = "user_id, created_at")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationType type;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false, length = 500)
    private String content;

    @Column(name = "target_url", updatable = false)
    private String targetUrl;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Notification create(
            Long userId,
            NotificationType type,
            String title,
            String content,
            String targetUrl
    ) {
        Notification notification = new Notification();
        notification.userId = userId;
        notification.type = type;
        notification.title = title;
        notification.content = content;
        notification.targetUrl = targetUrl;
        return notification;
    }

    public void markAsRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }

    public boolean isRead() {
        return readAt != null;
    }
}
