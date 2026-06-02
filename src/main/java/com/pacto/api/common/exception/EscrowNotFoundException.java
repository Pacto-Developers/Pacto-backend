package com.pacto.api.common.exception;

public class EscrowNotFoundException extends RuntimeException {
    public EscrowNotFoundException() {
        super("에스크로를 찾을 수 없습니다.");
    }
}
