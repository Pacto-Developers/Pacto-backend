package com.pacto.api.notification.event;

import com.pacto.api.notification.domain.NotificationType;

public record NotificationCreatedEvent(
        Long notificationId,
        Long userId,
        NotificationType type,
        String title,
        String content,
        String targetUrl
) {
}
