package com.pacto.api.notification.service;

import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.exception.InvalidNotificationPageRequestException;
import com.pacto.api.common.exception.NotificationNotFoundException;
import com.pacto.api.notification.domain.Notification;
import com.pacto.api.notification.domain.NotificationType;
import com.pacto.api.notification.dto.NotificationResponse;
import com.pacto.api.notification.event.NotificationCreatedEvent;
import com.pacto.api.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(Long userId, int page, int size) {
        validatePageRequest(page, size);
        PageRequest pageRequest = PageRequest.of(
                page - 1,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("notificationId")
                )
        );
        Page<Notification> notifications = notificationRepository.findByUserId(userId, pageRequest);
        return PageResponse.from(notifications, NotificationResponse::from);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(NotificationNotFoundException::new);
        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void notifyApplicationAccepted(Long bloggerId, Long campaignId, String campaignTitle) {
        save(
                bloggerId,
                NotificationType.APPLICATION_ACCEPTED,
                "캠페인 선정 안내",
                String.format("'%s' 캠페인에 선정되었습니다.", campaignTitle),
                "/campaigns/" + campaignId
        );
    }

    @Transactional
    public void notifyApplicationRejected(Long bloggerId, Long campaignId, String campaignTitle) {
        save(
                bloggerId,
                NotificationType.APPLICATION_REJECTED,
                "캠페인 선정 결과",
                String.format("'%s' 캠페인에 선정되지 않았습니다.", campaignTitle),
                "/campaigns/" + campaignId
        );
    }

    @Transactional
    public void notifyMissionApproved(Long bloggerId, Long missionId, String campaignTitle) {
        save(
                bloggerId,
                NotificationType.MISSION_APPROVED,
                "미션 승인 안내",
                String.format("'%s' 캠페인의 미션이 승인되었습니다.", campaignTitle),
                "/missions/" + missionId
        );
    }

    @Transactional
    public void notifyMissionRejected(Long bloggerId, Long missionId, String campaignTitle) {
        save(
                bloggerId,
                NotificationType.MISSION_REJECTED,
                "미션 반려 안내",
                String.format("'%s' 캠페인의 미션이 반려되었습니다.", campaignTitle),
                "/missions/" + missionId
        );
    }

    private void save(
            Long userId,
            NotificationType type,
            String title,
            String content,
            String targetUrl
    ) {
        Notification notification = notificationRepository.save(
                Notification.create(userId, type, title, content, targetUrl)
        );
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                notification.getNotificationId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetUrl()
        ));
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidNotificationPageRequestException();
        }
    }
}
