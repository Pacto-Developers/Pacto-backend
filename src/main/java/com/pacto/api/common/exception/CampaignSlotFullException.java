package com.pacto.api.common.exception;

public class CampaignSlotFullException extends RuntimeException {
    public CampaignSlotFullException() {
        super("모집 정원이 모두 찼습니다.");
    }
}