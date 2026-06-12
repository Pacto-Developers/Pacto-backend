package com.pacto.api.common.exception;

public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException() {
        super("이미 지원한 캠페인입니다.");
    }
}
