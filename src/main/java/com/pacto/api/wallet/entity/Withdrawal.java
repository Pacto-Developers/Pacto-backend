package com.pacto.api.wallet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawals")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_id")
    private Long withdrawalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false)
    private Wallet wallet;

    @Column(name = "amount", nullable = false, updatable = false)
    private int amount;

    @Column(name = "bank_name", nullable = false, updatable = false)
    private String bankName;

    @Column(name = "account_number", nullable = false, updatable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static Withdrawal create(Wallet wallet, int amount, String bankName, String accountNumber) {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.wallet = wallet;
        withdrawal.amount = amount;
        withdrawal.bankName = bankName;
        withdrawal.accountNumber = accountNumber;
        withdrawal.status = WithdrawalStatus.PENDING;
        return withdrawal;
    }
}
