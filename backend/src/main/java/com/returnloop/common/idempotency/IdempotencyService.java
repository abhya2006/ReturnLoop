package com.returnloop.common.idempotency;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Guards critical creation endpoints against duplicate submissions.
 *
 * <p>The fingerprint is a hash of the request payload: replaying the same key
 * with a different payload is rejected, replaying it with the same payload
 * returns the original response.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Looks for a previously completed operation.
     *
     * @return the stored response body when the same key and payload were seen before
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public <T> Optional<T> findReplay(UUID organizationId, String key, String operation, Object payload,
                                      Class<T> responseType) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String fingerprint = fingerprint(payload);
        return repository.findByOrganizationIdAndIdempotencyKey(organizationId, key)
                .filter(record -> record.isCompleted())
                .map(record -> {
                    if (!record.getRequestFingerprint().equals(fingerprint)) {
                        throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                                "Idempotency key was already used with a different request payload");
                    }
                    try {
                        return objectMapper.readValue(record.getResponseBody(), responseType);
                    } catch (JsonProcessingException ex) {
                        throw new ApiException(ErrorCode.INTERNAL_ERROR, "Stored idempotent response is unreadable", ex);
                    }
                });
    }

    /**
     * Stores the outcome of an operation so a replay can be served from cache.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remember(UUID organizationId, String key, String operation, Object payload, Object response,
                         int statusCode) {
        if (key == null || key.isBlank()) {
            return;
        }
        IdempotencyRecord record = repository
                .findByOrganizationIdAndIdempotencyKey(organizationId, key)
                .orElseGet(() -> {
                    IdempotencyRecord created = new IdempotencyRecord();
                    created.setOrganizationId(organizationId);
                    created.setIdempotencyKey(key);
                    return created;
                });
        record.setOperation(operation);
        record.setRequestFingerprint(fingerprint(payload));
        try {
            record.setResponseBody(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Unable to serialise idempotent response", ex);
        }
        record.setStatusCode(statusCode);
        record.setCompleted(true);
        repository.save(record);
    }

    private String fingerprint(Object payload) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Unable to fingerprint request payload", ex);
        }
    }
}
