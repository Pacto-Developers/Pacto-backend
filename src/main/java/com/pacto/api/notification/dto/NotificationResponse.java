package com.pacto.api.notification.dto;

import com.pacto.api.notification.domain.Notification;
import com.pacto.api.notification.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String content,
        String targetUrl,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetUrl(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
