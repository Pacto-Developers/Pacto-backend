package com.pacto.api.common.exception;

public class InvalidCampaignStatusException extends RuntimeException {
    public InvalidCampaignStatusException(String message) {
        super(message);
    }
}
