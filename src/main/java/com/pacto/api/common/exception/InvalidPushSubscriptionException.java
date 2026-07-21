package com.pacto.api.common.exception;

public class InvalidPushSubscriptionException extends RuntimeException {

    public InvalidPushSubscriptionException() {
        super("유효하지 않은 푸시 등록 정보입니다.");
    }
}
