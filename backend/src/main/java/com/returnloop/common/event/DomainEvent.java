package com.returnloop.common.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A domain event. Events are transport agnostic: they can be dispatched
 * in-process (default) or published to Kafka. Every event carries a stable
 * {@code eventId} so that consumers can guarantee idempotent handling.
 */
public record DomainEvent(
        UUID eventId,
        String type,
        UUID organizationId,
        String aggregateType,
        UUID aggregateId,
        String actor,
        Map<String, Object> payload,
        Instant occurredAt) {

    public static DomainEvent of(String type, UUID organizationId, String aggregateType, UUID aggregateId,
                                 String actor, Map<String, Object> payload) {
        return new DomainEvent(UUID.randomUUID(), type, organizationId, aggregateType, aggregateId,
                actor, payload == null ? Map.of() : Map.copyOf(payload), Instant.now());
    }
}
