package com.pacto.api.notification.service;

import com.pacto.api.common.exception.InvalidPushSubscriptionException;
import com.pacto.api.common.exception.PushSubscriptionNotFoundException;
import com.pacto.api.notification.domain.PushRegistrationType;
import com.pacto.api.notification.domain.PushSubscription;
import com.pacto.api.notification.dto.PushSubscriptionRequest;
import com.pacto.api.notification.dto.PushSubscriptionResponse;
import com.pacto.api.notification.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionServiceTest {

    @Mock PushSubscriptionRepository pushSubscriptionRepository;
    @InjectMocks PushSubscriptionService pushSubscriptionService;

    @Test
    void 새로운_FID를_등록한다() {
        PushSubscriptionRequest request = new PushSubscriptionRequest(PushRegistrationType.FID, "fid-1");
        when(pushSubscriptionRepository.findByRegistrationTypeAndRegistrationId(
                PushRegistrationType.FID, "fid-1"
        )).thenReturn(Optional.empty());
        when(pushSubscriptionRepository.save(any(PushSubscription.class)))
                .thenAnswer(invocation -> {
                    PushSubscription subscription = invocation.getArgument(0);
                    ReflectionTestUtils.setField(subscription, "pushSubscriptionId", 10L);
                    return subscription;
                });

        PushSubscriptionResponse response = pushSubscriptionService.register(1L, request);

        assertThat(response.pushSubscriptionId()).isEqualTo(10L);
        assertThat(response.registrationType()).isEqualTo(PushRegistrationType.FID);
        assertThat(response.active()).isTrue();
    }

    @Test
    void 기존_등록값은_현재_사용자에게_재할당하고_활성화한다() {
        PushSubscription subscription = PushSubscription.create(2L, PushRegistrationType.TOKEN, "token-1");
        subscription.deactivate();
        when(pushSubscriptionRepository.findByRegistrationTypeAndRegistrationId(
                PushRegistrationType.TOKEN, "token-1"
        )).thenReturn(Optional.of(subscription));
        when(pushSubscriptionRepository.save(subscription)).thenReturn(subscription);

        pushSubscriptionService.register(
                1L,
                new PushSubscriptionRequest(PushRegistrationType.TOKEN, "token-1")
        );

        assertThat(subscription.getUserId()).isEqualTo(1L);
        assertThat(subscription.isActive()).isTrue();
    }

    @Test
    void 자신의_등록값을_비활성화한다() {
        PushSubscription subscription = PushSubscription.create(1L, PushRegistrationType.FID, "fid-1");
        when(pushSubscriptionRepository.findByUserIdAndRegistrationTypeAndRegistrationId(
                1L, PushRegistrationType.FID, "fid-1"
        )).thenReturn(Optional.of(subscription));

        pushSubscriptionService.unregister(
                1L,
                new PushSubscriptionRequest(PushRegistrationType.FID, "fid-1")
        );

        assertThat(subscription.isActive()).isFalse();
    }

    @Test
    void 잘못된_등록값과_타인의_등록값을_거부한다() {
        assertThatThrownBy(() -> pushSubscriptionService.register(
                1L,
                new PushSubscriptionRequest(null, " ")
        )).isInstanceOf(InvalidPushSubscriptionException.class);

        when(pushSubscriptionRepository.findByUserIdAndRegistrationTypeAndRegistrationId(
                1L, PushRegistrationType.FID, "fid-2"
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pushSubscriptionService.unregister(
                1L,
                new PushSubscriptionRequest(PushRegistrationType.FID, "fid-2")
        )).isInstanceOf(PushSubscriptionNotFoundException.class);
    }
}
