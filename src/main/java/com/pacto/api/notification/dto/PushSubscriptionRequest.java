package com.pacto.api.notification.dto;

import com.pacto.api.notification.domain.PushRegistrationType;

public record PushSubscriptionRequest(
        PushRegistrationType registrationType,
        String registrationId
) {
}
