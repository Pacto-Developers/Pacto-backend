package com.pacto.api.common.exception;

public class MissingRoleException extends RuntimeException {
    public MissingRoleException() {
        super("role값이 없습니다.");
    }
}
