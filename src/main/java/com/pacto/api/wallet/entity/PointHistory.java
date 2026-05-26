package com.pacto.api.wallet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_histories")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id", updatable = false)
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false)
    private Wallet wallet;

    @Column(name = "amount", nullable = false, updatable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PointHistoryType type;

    @Column(name = "reference_id", updatable = false)
    private Long referenceId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static PointHistory create(Wallet wallet, int amount, PointHistoryType type, Long referenceId) {
        PointHistory history = new PointHistory();
        history.wallet = wallet;
        history.amount = amount;
        history.type = type;
        history.referenceId = referenceId;
        return history;
    }
}
