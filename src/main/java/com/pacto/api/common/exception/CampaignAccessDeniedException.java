package com.pacto.api.common.exception;

public class CampaignAccessDeniedException extends RuntimeException {
    public CampaignAccessDeniedException() {
        super("캠페인 소유자만 접근할 수 있습니다.");
    }
}
