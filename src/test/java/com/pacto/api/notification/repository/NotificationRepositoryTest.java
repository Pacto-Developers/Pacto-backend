package com.pacto.api.notification.repository;

import com.pacto.api.notification.domain.Notification;
import com.pacto.api.notification.domain.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NotificationRepositoryTest {

    @Autowired NotificationRepository notificationRepository;

    @Test
    void 사용자별_알림을_최신순으로_조회한다() {
        notificationRepository.saveAndFlush(Notification.create(
                1L,
                NotificationType.APPLICATION_ACCEPTED,
                "선정 안내",
                "선정되었습니다.",
                null
        ));
        notificationRepository.saveAndFlush(Notification.create(
                1L,
                NotificationType.MISSION_APPROVED,
                "승인 안내",
                "승인되었습니다.",
                null
        ));
        notificationRepository.saveAndFlush(Notification.create(
                2L,
                NotificationType.APPLICATION_REJECTED,
                "미선정 안내",
                "선정되지 않았습니다.",
                null
        ));

        var result = notificationRepository.findByUserId(
                1L,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(notification -> notification.getUserId().equals(1L));
        assertThat(result.getContent())
                .extracting(Notification::getType)
                .containsExactly(NotificationType.MISSION_APPROVED, NotificationType.APPLICATION_ACCEPTED);
    }

    @Test
    void 알림은_소유자와_식별자가_모두_일치해야_조회된다() {
        Notification notification = notificationRepository.save(Notification.create(
                1L,
                NotificationType.MISSION_REJECTED,
                "반려 안내",
                "반려되었습니다.",
                null
        ));

        assertThat(notificationRepository.findByNotificationIdAndUserId(
                notification.getNotificationId(),
                1L
        )).isPresent();
        assertThat(notificationRepository.findByNotificationIdAndUserId(
                notification.getNotificationId(),
                2L
        )).isEmpty();
    }
}
