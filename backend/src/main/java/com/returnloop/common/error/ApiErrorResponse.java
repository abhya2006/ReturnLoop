package com.returnloop.common.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Uniform error envelope returned by every endpoint.
 */
@Schema(name = "ApiError", description = "Standard error payload")
public record ApiErrorResponse(
        @Schema(description = "Server timestamp of the failure") Instant timestamp,
        @Schema(description = "HTTP status code") int status,
        @Schema(description = "Stable machine readable error code") String code,
        @Schema(description = "Human readable message") String message,
        @Schema(description = "Request path that failed") String path,
        @Schema(description = "Field level validation errors") Map<String, List<String>> fieldErrors,
        @Schema(description = "Additional context") Map<String, Object> details,
        @Schema(description = "Correlation id for support requests") String traceId) {
}
