package com.pacto.api.common.exception;

public class ApplicationNotFoundException extends RuntimeException {
    public ApplicationNotFoundException() {
        super("지원을 찾을 수 없습니다.");
    }
}
