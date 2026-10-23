package com.returnloop.common.error;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Business exception carrying a stable {@link ErrorCode} plus optional field
 * level details. Translated to an HTTP response by {@link GlobalExceptionHandler}.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details = new LinkedHashMap<>();

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage());
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ApiException detail(String key, Object value) {
        details.put(key, value);
        return this;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public static ApiException notFound(String what, Object id) {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, what + " " + id + " was not found");
    }

    public static ApiException conflict(String message) {
        return new ApiException(ErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }

    public static ApiException invalidState(String message) {
        return new ApiException(ErrorCode.INVALID_STATE_TRANSITION, message);
    }
}
