package com.pacto.api.notification.service;

import com.pacto.api.common.exception.InvalidPushSubscriptionException;
import com.pacto.api.common.exception.PushSubscriptionNotFoundException;
import com.pacto.api.notification.domain.PushRegistrationType;
import com.pacto.api.notification.domain.PushSubscription;
import com.pacto.api.notification.dto.PushSubscriptionRequest;
import com.pacto.api.notification.dto.PushSubscriptionResponse;
import com.pacto.api.notification.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private static final int MAX_REGISTRATION_ID_LENGTH = 2048;

    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Transactional
    public PushSubscriptionResponse register(Long userId, PushSubscriptionRequest request) {
        validate(request);

        PushRegistrationType type = request.registrationType();
        String registrationId = request.registrationId().trim();
        PushSubscription subscription = pushSubscriptionRepository
                .findByRegistrationTypeAndRegistrationId(type, registrationId)
                .map(existing -> {
                    existing.activateFor(userId);
                    return existing;
                })
                .orElseGet(() -> PushSubscription.create(userId, type, registrationId));

        return PushSubscriptionResponse.from(pushSubscriptionRepository.save(subscription));
    }

    @Transactional
    public void unregister(Long userId, PushSubscriptionRequest request) {
        validate(request);

        PushSubscription subscription = pushSubscriptionRepository
                .findByUserIdAndRegistrationTypeAndRegistrationId(
                        userId,
                        request.registrationType(),
                        request.registrationId().trim()
                )
                .orElseThrow(PushSubscriptionNotFoundException::new);
        subscription.deactivate();
    }

    private void validate(PushSubscriptionRequest request) {
        if (request == null
                || request.registrationType() == null
                || request.registrationId() == null
                || request.registrationId().isBlank()
                || request.registrationId().trim().length() > MAX_REGISTRATION_ID_LENGTH) {
            throw new InvalidPushSubscriptionException();
        }
    }
}
