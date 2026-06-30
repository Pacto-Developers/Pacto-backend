package com.pacto.api.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "advertiser_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvertiserProfile {

    @Id
    @Column(name = "advertiser_id")
    private Long advertiserId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "advertiser_id")
    private User user;

    @Column(name = "manager_name")
    private String managerName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "business_number")
    private String businessNumber;

    private String contact;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "account_holder")
    private String accountHolder;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static AdvertiserProfile create(User user) {
        AdvertiserProfile profile = new AdvertiserProfile();
        profile.user = user;
        return profile;
    }

    public void updateProfile(
            String managerName,
            String companyName,
            String businessNumber,
            String contact,
            String brandName,
            String bankName,
            String accountNumber,
            String accountHolder
    ) {
        this.managerName = managerName;
        this.companyName = companyName;
        this.businessNumber = businessNumber;
        this.contact = contact;
        this.brandName = brandName;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }
}
