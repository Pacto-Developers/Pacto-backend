package com.pacto.api.auth.dto;

import com.pacto.api.auth.entity.AdvertiserProfile;
import com.pacto.api.auth.entity.BloggerProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeResponse {

    private Long userId;
    private String email;
    private String role;
    private BloggerProfileResponse bloggerProfile;
    private AdvertiserProfileResponse advertiserProfile;

    public static MeResponse ofBlogger(
            Long userId,
            String email,
            String role,
            BloggerProfile bloggerProfile,
            String profileImageDownloadUrl
    ) {
        return new MeResponse(
                userId,
                email,
                role,
                BloggerProfileResponse.from(bloggerProfile, profileImageDownloadUrl),
                null
        );
    }

    public static MeResponse ofAdvertiser(
            Long userId,
            String email,
            String role,
            AdvertiserProfile advertiserProfile
    ) {
        return new MeResponse(
                userId,
                email,
                role,
                null,
                AdvertiserProfileResponse.from(advertiserProfile)
        );
    }

    @Getter
    @AllArgsConstructor
    public static class BloggerProfileResponse {
        private String name;
        private String blogUrl;
        private String contact;
        private String nickname;
        private String bankName;
        private String accountNumber;
        private String accountHolder;
        private String profileImageUrl;
        private String profileImageDownloadUrl;

        private static BloggerProfileResponse from(BloggerProfile profile, String profileImageDownloadUrl) {
            return new BloggerProfileResponse(
                    profile.getName(),
                    profile.getBlogUrl(),
                    profile.getContact(),
                    profile.getNickname(),
                    profile.getBankName(),
                    profile.getAccountNumber(),
                    profile.getAccountHolder(),
                    profile.getProfileImageUrl(),
                    profileImageDownloadUrl
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class AdvertiserProfileResponse {
        private String managerName;
        private String companyName;
        private String businessNumber;
        private String contact;
        private String brandName;
        private String bankName;
        private String accountNumber;
        private String accountHolder;

        private static AdvertiserProfileResponse from(AdvertiserProfile profile) {
            return new AdvertiserProfileResponse(
                    profile.getManagerName(),
                    profile.getCompanyName(),
                    profile.getBusinessNumber(),
                    profile.getContact(),
                    profile.getBrandName(),
                    profile.getBankName(),
                    profile.getAccountNumber(),
                    profile.getAccountHolder()
            );
        }
    }
}
