package com.pacto.api.escrow.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "escrow_ledgers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EscrowLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "escrow_id")
    private Long escrowId;

    @Column(name = "campaign_id", nullable = false, updatable = false)
    private Long campaignId;

    @Column(name = "blogger_id", nullable = false, updatable = false)
    private Long bloggerId;

    @Column(name = "amount", nullable = false, updatable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscrowStatus status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static EscrowLedger create(Long campaignId, Long bloggerId, int amount) {
        EscrowLedger escrow = new EscrowLedger();
        escrow.campaignId = campaignId;
        escrow.bloggerId = bloggerId;
        escrow.amount = amount;
        escrow.status = EscrowStatus.LOCKED;
        return escrow;
    }
}
