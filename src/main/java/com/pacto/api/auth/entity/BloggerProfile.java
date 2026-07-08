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
@Table(name = "blogger_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BloggerProfile {

    @Id
    @Column(name = "blogger_id")
    private Long bloggerId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "blogger_id")
    private User user;

    private String name;

    @Column(name = "blog_url")
    private String blogUrl;

    private String contact;

    private String nickname;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "account_holder")
    private String accountHolder;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static BloggerProfile create(User user) {
        BloggerProfile profile = new BloggerProfile();
        profile.user = user;
        return profile;
    }

    public void updateProfile(
            String name,
            String blogUrl,
            String contact,
            String nickname,
            String bankName,
            String accountNumber,
            String accountHolder,
            String profileImageUrl
    ) {
        this.name = name;
        this.blogUrl = blogUrl;
        this.contact = contact;
        this.nickname = nickname;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateProfilePartially(
            String name,
            String blogUrl,
            String contact,
            String nickname,
            String bankName,
            String accountNumber,
            String accountHolder,
            String profileImageUrl
    ) {
        this.name = updateIfPresent(this.name, name);
        this.blogUrl = updateIfPresent(this.blogUrl, blogUrl);
        this.contact = updateIfPresent(this.contact, contact);
        this.nickname = updateIfPresent(this.nickname, nickname);
        this.bankName = updateIfPresent(this.bankName, bankName);
        this.accountNumber = updateIfPresent(this.accountNumber, accountNumber);
        this.accountHolder = updateIfPresent(this.accountHolder, accountHolder);
        this.profileImageUrl = updateIfPresent(this.profileImageUrl, profileImageUrl);
    }

    private String updateIfPresent(String currentValue, String newValue) {
        return newValue != null ? newValue : currentValue;
    }
}
