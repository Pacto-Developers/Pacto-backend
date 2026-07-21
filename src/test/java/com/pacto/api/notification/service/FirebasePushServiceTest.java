package com.pacto.api.notification.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.pacto.api.notification.domain.NotificationType;
import com.pacto.api.notification.domain.PushRegistrationType;
import com.pacto.api.notification.domain.PushSubscription;
import com.pacto.api.notification.event.NotificationCreatedEvent;
import com.pacto.api.notification.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebasePushServiceTest {

    @Mock FirebaseMessaging firebaseMessaging;
    @Mock PushSubscriptionRepository pushSubscriptionRepository;
    @InjectMocks FirebasePushService firebasePushService;

    @Test
    void 사용자의_FID와_TOKEN으로_푸시를_발송한다() throws FirebaseMessagingException {
        PushSubscription fid = subscription(1L, PushRegistrationType.FID, "fid-1", 10L);
        PushSubscription token = subscription(1L, PushRegistrationType.TOKEN, "token-1", 11L);
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(pushSubscriptionRepository.findByUserIdAndActiveTrue(1L))
                .thenReturn(List.of(fid, token));
        when(firebaseMessaging.sendEach(anyList())).thenReturn(batchResponse);
        when(batchResponse.getResponses()).thenReturn(List.of(
                mock(SendResponse.class),
                mock(SendResponse.class)
        ));

        firebasePushService.sendToUser(event());

        verify(firebaseMessaging).sendEach(anyList());
        assertThat(fid.isActive()).isTrue();
        assertThat(token.isActive()).isTrue();
    }

    @Test
    void 만료된_등록값을_비활성화한다() throws FirebaseMessagingException {
        PushSubscription subscription = subscription(1L, PushRegistrationType.FID, "fid-1", 10L);
        BatchResponse batchResponse = mock(BatchResponse.class);
        SendResponse sendResponse = mock(SendResponse.class);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(pushSubscriptionRepository.findByUserIdAndActiveTrue(1L))
                .thenReturn(List.of(subscription));
        when(firebaseMessaging.sendEach(anyList())).thenReturn(batchResponse);
        when(batchResponse.getResponses()).thenReturn(List.of(sendResponse));
        when(sendResponse.getException()).thenReturn(exception);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);

        firebasePushService.sendToUser(event());

        assertThat(subscription.isActive()).isFalse();
    }

    private PushSubscription subscription(
            Long userId,
            PushRegistrationType type,
            String registrationId,
            Long id
    ) {
        PushSubscription subscription = PushSubscription.create(userId, type, registrationId);
        ReflectionTestUtils.setField(subscription, "pushSubscriptionId", id);
        return subscription;
    }

    private NotificationCreatedEvent event() {
        return new NotificationCreatedEvent(
                100L,
                1L,
                NotificationType.APPLICATION_ACCEPTED,
                "선정 안내",
                "캠페인에 선정되었습니다.",
                "/campaigns/20"
        );
    }
}
