package com.pacto.api.common.exception;

public class MissionAccessDeniedException extends RuntimeException {
    public MissionAccessDeniedException() {
        super("본인의 미션만 접근할 수 있습니다.");
    }
}
