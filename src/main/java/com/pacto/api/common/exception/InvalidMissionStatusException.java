package com.pacto.api.common.exception;

public class InvalidMissionStatusException extends RuntimeException {
    public InvalidMissionStatusException(String message) {
        super(message);
    }
}