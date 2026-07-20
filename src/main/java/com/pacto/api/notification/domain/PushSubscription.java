package com.pacto.api.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "push_subscriptions",
        indexes = @Index(name = "idx_push_subscriptions_user_active", columnList = "user_id, active"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_push_subscriptions_type_registration",
                columnNames = {"registration_type", "registration_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_subscription_id")
    private Long pushSubscriptionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", nullable = false, updatable = false)
    private PushRegistrationType registrationType;

    @Column(name = "registration_id", nullable = false, updatable = false, length = 2048)
    private String registrationId;

    @Column(nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static PushSubscription create(
            Long userId,
            PushRegistrationType registrationType,
            String registrationId
    ) {
        PushSubscription subscription = new PushSubscription();
        subscription.userId = userId;
        subscription.registrationType = registrationType;
        subscription.registrationId = registrationId;
        subscription.active = true;
        return subscription;
    }

    public void activateFor(Long userId) {
        this.userId = userId;
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
    }
}
