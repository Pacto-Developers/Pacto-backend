package com.pacto.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileImageUploadResponse {

    private Long userId;
    private String profileImageKey;
}
