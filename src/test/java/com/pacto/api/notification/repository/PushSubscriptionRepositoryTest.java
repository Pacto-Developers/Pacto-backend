package com.pacto.api.notification.repository;

import com.pacto.api.notification.domain.PushRegistrationType;
import com.pacto.api.notification.domain.PushSubscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PushSubscriptionRepositoryTest {

    @Autowired PushSubscriptionRepository pushSubscriptionRepository;

    @Test
    void 사용자별_활성_등록값만_조회한다() {
        PushSubscription active = PushSubscription.create(1L, PushRegistrationType.FID, "fid-active");
        PushSubscription inactive = PushSubscription.create(1L, PushRegistrationType.TOKEN, "token-inactive");
        inactive.deactivate();
        pushSubscriptionRepository.save(active);
        pushSubscriptionRepository.save(inactive);
        pushSubscriptionRepository.save(PushSubscription.create(
                2L,
                PushRegistrationType.FID,
                "fid-other-user"
        ));

        var result = pushSubscriptionRepository.findByUserIdAndActiveTrue(1L);

        assertThat(result)
                .extracting(PushSubscription::getRegistrationId)
                .containsExactly("fid-active");
    }

    @Test
    void 타입과_등록값으로_기존_구독을_찾는다() {
        pushSubscriptionRepository.saveAndFlush(
                PushSubscription.create(1L, PushRegistrationType.FID, "fid-1")
        );

        assertThat(pushSubscriptionRepository.findByRegistrationTypeAndRegistrationId(
                PushRegistrationType.FID,
                "fid-1"
        )).isPresent();
        assertThat(pushSubscriptionRepository.findByRegistrationTypeAndRegistrationId(
                PushRegistrationType.TOKEN,
                "fid-1"
        )).isEmpty();
    }
}
