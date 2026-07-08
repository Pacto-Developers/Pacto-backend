package com.pacto.api.common.exception;

public class CampaignNotOpenException extends RuntimeException {
    public CampaignNotOpenException() {
        super("모집 중인 캠페인에만 지원할 수 있습니다.");
    }

    public CampaignNotOpenException(String message) {
        super(message);
    }
}
