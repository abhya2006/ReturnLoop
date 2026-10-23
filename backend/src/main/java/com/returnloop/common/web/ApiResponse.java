package com.returnloop.common.web;

import java.time.Instant;

/**
 * Envelope for successful single-resource responses.
 */
public record ApiResponse<T>(T data, Instant timestamp) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, Instant.now());
    }
}
