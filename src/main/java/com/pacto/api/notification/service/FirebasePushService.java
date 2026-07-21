package com.pacto.api.notification.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.pacto.api.notification.domain.PushRegistrationType;
import com.pacto.api.notification.domain.PushSubscription;
import com.pacto.api.notification.event.NotificationCreatedEvent;
import com.pacto.api.notification.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class FirebasePushService {

    private static final int MAX_BATCH_SIZE = 500;

    private final FirebaseMessaging firebaseMessaging;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Transactional
    public void sendToUser(NotificationCreatedEvent event) {
        List<PushSubscription> subscriptions =
                pushSubscriptionRepository.findByUserIdAndActiveTrue(event.userId());

        for (int from = 0; from < subscriptions.size(); from += MAX_BATCH_SIZE) {
            int to = Math.min(from + MAX_BATCH_SIZE, subscriptions.size());
            sendBatch(event, subscriptions.subList(from, to));
        }
    }

    private void sendBatch(NotificationCreatedEvent event, List<PushSubscription> subscriptions) {
        List<Message> messages = subscriptions.stream()
                .map(subscription -> createMessage(event, subscription))
                .toList();

        try {
            BatchResponse batchResponse = firebaseMessaging.sendEach(messages);
            deactivateInvalidRegistrations(subscriptions, batchResponse.getResponses());
            log.info(
                    "FCM push completed: notificationId={}, success={}, failure={}",
                    event.notificationId(),
                    batchResponse.getSuccessCount(),
                    batchResponse.getFailureCount()
            );
        } catch (FirebaseMessagingException e) {
            log.error("FCM push failed: notificationId={}", event.notificationId(), e);
        }
    }

    private Message createMessage(NotificationCreatedEvent event, PushSubscription subscription) {
        Message.Builder builder = Message.builder()
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(event.title())
                        .setBody(event.content())
                        .build())
                .putData("notificationId", event.notificationId().toString())
                .putData("type", event.type().name());

        if (event.targetUrl() != null) {
            builder.putData("targetUrl", event.targetUrl());
        }

        if (subscription.getRegistrationType() == PushRegistrationType.FID) {
            return builder.setFid(subscription.getRegistrationId()).build();
        }
        return builder.setToken(subscription.getRegistrationId()).build();
    }

    private void deactivateInvalidRegistrations(
            List<PushSubscription> subscriptions,
            List<SendResponse> responses
    ) {
        List<Long> deactivatedIds = new ArrayList<>();

        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (response.isSuccessful() || response.getException() == null) {
                continue;
            }

            MessagingErrorCode errorCode = response.getException().getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED
                    || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                PushSubscription subscription = subscriptions.get(i);
                subscription.deactivate();
                deactivatedIds.add(subscription.getPushSubscriptionId());
            }
        }

        if (!deactivatedIds.isEmpty()) {
            log.info("Deactivated invalid push registrations: ids={}", deactivatedIds);
        }
    }
}
