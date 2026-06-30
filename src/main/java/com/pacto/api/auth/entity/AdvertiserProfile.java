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

    private String managerName;

    private String companyName;

    private String businessNumber;

    private String contact;

    private String brandName;

    private String bankName;

    private String accountNumber;

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
}
