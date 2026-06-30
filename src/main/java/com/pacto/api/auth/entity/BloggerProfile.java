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

    private String blogUrl;

    private String contact;

    private String nickname;

    private String bankName;

    private String accountNumber;

    private String accountHolder;

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
}
