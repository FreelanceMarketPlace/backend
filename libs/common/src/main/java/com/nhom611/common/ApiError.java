package com.nhom611.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String requestId,
        Map<String, Object> details
) {
    public static ApiError of(int status, String error, String message, String path, String requestId) {
        return new ApiError(Instant.now(), status, error, message, path, requestId, null);
    }
}
