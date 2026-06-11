package com.pacto.api.common.exception;

public class InvalidRoleException extends RuntimeException {
    public InvalidRoleException() {
        super("유효하지 않은 role입니다.");
    }
}
