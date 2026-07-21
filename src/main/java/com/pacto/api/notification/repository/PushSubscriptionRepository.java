package com.pacto.api.notification.repository;

import com.pacto.api.notification.domain.PushRegistrationType;
import com.pacto.api.notification.domain.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByRegistrationTypeAndRegistrationId(
            PushRegistrationType registrationType,
            String registrationId
    );

    Optional<PushSubscription> findByUserIdAndRegistrationTypeAndRegistrationId(
            Long userId,
            PushRegistrationType registrationType,
            String registrationId
    );

    List<PushSubscription> findByUserIdAndActiveTrue(Long userId);
}
