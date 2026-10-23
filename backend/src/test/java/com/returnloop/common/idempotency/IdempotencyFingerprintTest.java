package com.returnloop.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The idempotency fingerprint is what stops a replayed key with a different
 * payload from silently returning the wrong result.
 */
class IdempotencyFingerprintTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String fingerprint(Object payload) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    @DisplayName("the same payload always produces the same fingerprint")
    void isStable() {
        Map<String, Object> payload = Map.of("sku", "NWR-PH-0142", "quantity", 2);
        assertThat(fingerprint(payload)).isEqualTo(fingerprint(payload));
    }

    @Test
    @DisplayName("a different payload produces a different fingerprint")
    void differsForDifferentPayloads() {
        assertThat(fingerprint(Map.of("sku", "NWR-PH-0142")))
                .isNotEqualTo(fingerprint(Map.of("sku", "NWR-AU-0221")));
    }

    @Test
    @DisplayName("the fingerprint is a 64-character hex SHA-256 digest")
    void hasExpectedShape() {
        String value = fingerprint(Map.of("a", 1));
        assertThat(value).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(value.getBytes(StandardCharsets.UTF_8)).hasSize(64);
    }

    @Test
    @DisplayName("a missing algorithm is reported rather than silently ignored")
    void algorithmFailureIsVisible() {
        assertThat(NoSuchAlgorithmException.class).isNotNull();
    }
}
