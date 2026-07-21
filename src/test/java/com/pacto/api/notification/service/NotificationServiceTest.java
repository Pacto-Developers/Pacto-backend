package com.pacto.api.notification.service;

import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.exception.InvalidNotificationPageRequestException;
import com.pacto.api.common.exception.NotificationNotFoundException;
import com.pacto.api.notification.domain.Notification;
import com.pacto.api.notification.domain.NotificationType;
import com.pacto.api.notification.dto.NotificationResponse;
import com.pacto.api.notification.repository.NotificationRepository;
import com.pacto.api.notification.event.NotificationCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks NotificationService notificationService;

    @Test
    void 내_알림을_최신순으로_조회한다() {
        Notification notification = createNotification(NotificationType.APPLICATION_ACCEPTED);
        PageRequest pageRequest = PageRequest.of(
                0,
                20,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("notificationId")
                )
        );
        when(notificationRepository.findByUserId(1L, pageRequest))
                .thenReturn(new PageImpl<>(List.of(notification), pageRequest, 1));

        PageResponse<NotificationResponse> response = notificationService.getMyNotifications(1L, 1, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).notificationId()).isEqualTo(10L);
        assertThat(response.getCurrentPage()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }

    @Test
    void 자신의_알림을_읽음_처리한다() {
        Notification notification = createNotification(NotificationType.MISSION_APPROVED);
        when(notificationRepository.findByNotificationIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.markAsRead(10L, 1L);

        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    void 타인의_알림은_읽음_처리할_수_없다() {
        when(notificationRepository.findByNotificationIdAndUserId(10L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(10L, 2L))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void 잘못된_페이지_요청을_거부한다() {
        assertThatThrownBy(() -> notificationService.getMyNotifications(1L, 0, 20))
                .isInstanceOf(InvalidNotificationPageRequestException.class);
        assertThatThrownBy(() -> notificationService.getMyNotifications(1L, 1, 101))
                .isInstanceOf(InvalidNotificationPageRequestException.class);
    }

    @Test
    void 선정_알림을_저장한다() {
        stubNotificationSave();
        notificationService.notifyApplicationAccepted(1L, 20L, "팩토 체험단");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification notification = captor.getValue();
        assertThat(notification.getUserId()).isEqualTo(1L);
        assertThat(notification.getType()).isEqualTo(NotificationType.APPLICATION_ACCEPTED);
        assertThat(notification.getContent()).contains("팩토 체험단");
        assertThat(notification.getTargetUrl()).isEqualTo("/campaigns/20");
        verify(eventPublisher).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    void 미선정과_미션_승인_반려_알림을_각각_저장한다() {
        stubNotificationSave();
        notificationService.notifyApplicationRejected(1L, 20L, "캠페인");
        notificationService.notifyMissionApproved(1L, 30L, "캠페인");
        notificationService.notifyMissionRejected(1L, 30L, "캠페인");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getType)
                .containsExactly(
                        NotificationType.APPLICATION_REJECTED,
                        NotificationType.MISSION_APPROVED,
                        NotificationType.MISSION_REJECTED
                );
    }

    private Notification createNotification(NotificationType type) {
        Notification notification = Notification.create(1L, type, "제목", "내용", null);
        ReflectionTestUtils.setField(notification, "notificationId", 10L);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.now());
        return notification;
    }

    private void stubNotificationSave() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);
                    ReflectionTestUtils.setField(notification, "notificationId", 10L);
                    return notification;
                });
    }
}
