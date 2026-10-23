package com.returnloop.common.error;

import org.springframework.http.HttpStatus;

/**
 * Central catalogue of application error codes. Messages are safe to expose
 * to API clients; internal detail is never leaked through them.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "The requested state transition is not allowed"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Resource already exists"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token has expired"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    CROSS_TENANT_ACCESS(HttpStatus.FORBIDDEN, "Resource belongs to another organization"),
    CAPACITY_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "Vehicle capacity would be exceeded"),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, "Idempotency key was already used with a different payload"),
    DUPLICATE_ASSIGNMENT(HttpStatus.CONFLICT, "An active job already exists for this item"),
    FILE_REJECTED(HttpStatus.BAD_REQUEST, "Uploaded file was rejected"),
    IMPORT_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Import could not be completed"),
    EMAIL_DELIVERY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "The email could not be sent. Please try again later"),
    IDENTITY_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is temporarily unavailable"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "The record was modified by another user, please reload"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
