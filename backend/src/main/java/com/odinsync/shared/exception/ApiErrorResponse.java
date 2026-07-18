package com.odinsync.shared.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
        String code,
        String message,
        Integer status,
        Instant timestamp
) {
    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, null, Instant.now());
    }

    public static ApiErrorResponse of(String code, String message, HttpStatus status) {
        return new ApiErrorResponse(code, message, status.value(), Instant.now());
    }
}
