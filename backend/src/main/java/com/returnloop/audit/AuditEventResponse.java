package com.returnloop.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String actor,
        String actorRole,
        String action,
        String entityType,
        UUID entityId,
        String summary,
        String ipAddress,
        Instant occurredAt) {

    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActor(),
                event.getActorRole(),
                event.getAction(),
                event.getEntityType(),
                event.getEntityId(),
                event.getSummary(),
                event.getIpAddress(),
                event.getCreatedAt());
    }
}
