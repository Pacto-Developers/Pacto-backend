package com.pacto.api.notification.controller;

import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.response.CommonResponse;
import com.pacto.api.notification.domain.Notification;
import com.pacto.api.notification.domain.NotificationType;
import com.pacto.api.notification.dto.NotificationResponse;
import com.pacto.api.notification.dto.PushSubscriptionRequest;
import com.pacto.api.notification.dto.PushSubscriptionResponse;
import com.pacto.api.notification.domain.PushRegistrationType;
import com.pacto.api.notification.service.NotificationService;
import com.pacto.api.notification.service.PushSubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock NotificationService notificationService;
    @Mock PushSubscriptionService pushSubscriptionService;
    @Mock Authentication authentication;
    @InjectMocks NotificationController notificationController;

    @Test
    void 내_알림_목록을_공통_응답으로_반환한다() {
        Notification notification = Notification.create(
                1L,
                NotificationType.APPLICATION_ACCEPTED,
                "선정 안내",
                "선정되었습니다.",
                null
        );
        ReflectionTestUtils.setField(notification, "notificationId", 10L);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.now());
        PageResponse<NotificationResponse> page = PageResponse.from(
                new PageImpl<>(List.of(notification)),
                NotificationResponse::from
        );
        when(authentication.getPrincipal()).thenReturn(1L);
        when(notificationService.getMyNotifications(1L, 1, 20)).thenReturn(page);

        ResponseEntity<CommonResponse<PageResponse<NotificationResponse>>> response =
                notificationController.getMyNotifications(authentication, 1, 20);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().getContent()).hasSize(1);
    }

    @Test
    void 웹푸시_등록과_해제를_처리한다() {
        PushSubscriptionRequest request = new PushSubscriptionRequest(
                PushRegistrationType.FID,
                "fid-1"
        );
        PushSubscriptionResponse subscriptionResponse = new PushSubscriptionResponse(
                10L,
                PushRegistrationType.FID,
                true,
                LocalDateTime.now()
        );
        when(authentication.getPrincipal()).thenReturn(1L);
        when(pushSubscriptionService.register(1L, request)).thenReturn(subscriptionResponse);

        var registerResponse = notificationController.registerPushSubscription(authentication, request);
        var unregisterResponse = notificationController.unregisterPushSubscription(authentication, request);

        assertThat(registerResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().data().pushSubscriptionId()).isEqualTo(10L);
        assertThat(unregisterResponse.getStatusCode().is2xxSuccessful()).isTrue();
        org.mockito.Mockito.verify(pushSubscriptionService).unregister(1L, request);
    }
}
