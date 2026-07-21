package com.pacto.api.common.exception;

public class InvalidNotificationPageRequestException extends RuntimeException {

    public InvalidNotificationPageRequestException() {
        super("페이지는 1 이상, 페이지 크기는 1 이상 100 이하여야 합니다.");
    }
}
