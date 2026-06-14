package com.pacto.api.common.exception;

public class MissingRoleException extends RuntimeException {
    public MissingRoleException() {
        super("role은 필수입니다.");
    }
}
