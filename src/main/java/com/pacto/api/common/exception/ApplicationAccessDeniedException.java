package com.pacto.api.common.exception;

public class ApplicationAccessDeniedException extends RuntimeException {
    public ApplicationAccessDeniedException() {
        super("본인의 지원만 취소할 수 있습니다.");
    }
}
