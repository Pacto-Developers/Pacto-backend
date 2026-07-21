package com.pacto.api.common.exception;

public class ProfileImageAccessDeniedException extends RuntimeException {
    public ProfileImageAccessDeniedException() {
        super("블로거만 프로필 이미지를 업로드할 수 있습니다.");
    }
}
