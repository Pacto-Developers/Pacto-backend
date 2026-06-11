package com.pacto.api.common.exception;

public class RoleMismatchException extends RuntimeException {
    public RoleMismatchException() {
        super("로그인 role이 일치하지 않습니다.");
    }
}
