package com.pacto.api.common.exception;

public class CampaignSlotFullException extends RuntimeException {
    public CampaignSlotFullException() {
        super("모집이 마감된 캠페인입니다.");
    }
}