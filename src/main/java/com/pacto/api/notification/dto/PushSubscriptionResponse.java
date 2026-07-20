package com.pacto.api.notification.dto;

import com.pacto.api.notification.domain.PushRegistrationType;
import com.pacto.api.notification.domain.PushSubscription;

import java.time.LocalDateTime;

public record PushSubscriptionResponse(
        Long pushSubscriptionId,
        PushRegistrationType registrationType,
        boolean active,
        LocalDateTime updatedAt
) {
    public static PushSubscriptionResponse from(PushSubscription subscription) {
        return new PushSubscriptionResponse(
                subscription.getPushSubscriptionId(),
                subscription.getRegistrationType(),
                subscription.isActive(),
                subscription.getUpdatedAt()
        );
    }
}
