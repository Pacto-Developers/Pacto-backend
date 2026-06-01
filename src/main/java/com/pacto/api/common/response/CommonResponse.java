package com.pacto.api.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

public record CommonResponse<T>(
        boolean success,
        String message,
        T data,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {
    public static <T> CommonResponse<T> success(String message, T data) {
        return new CommonResponse<>(true, message, data, LocalDateTime.now());
    }

    public static CommonResponse<Map<String, Object>> success(String message) {
        return new CommonResponse<>(true, message, Map.of(), LocalDateTime.now());
    }

    public static CommonResponse<Map<String, Object>> failure(String message) {
        return new CommonResponse<>(false, message, Map.of(), LocalDateTime.now());
    }
}
