package com.pacto.api.auth.dto;

import lombok.Getter;

@Getter
public class SignupRequest {

    private String email;
    private String password;
    private String role;
    private BloggerProfileRequest bloggerProfile;
    private AdvertiserProfileRequest advertiserProfile;

    @Getter
    public static class BloggerProfileRequest {
        private String name;
        private String blogUrl;
        private String contact;
        private String nickname;
        private String bankName;
        private String accountNumber;
        private String accountHolder;
        private String profileImageUrl;
    }

    @Getter
    public static class AdvertiserProfileRequest {
        private String managerName;
        private String companyName;
        private String businessNumber;
        private String contact;
        private String brandName;
        private String bankName;
        private String accountNumber;
        private String accountHolder;
    }
}
