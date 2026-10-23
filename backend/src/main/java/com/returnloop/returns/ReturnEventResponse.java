package com.returnloop.returns;

import java.time.Instant;
import java.util.UUID;

public record ReturnEventResponse(
        UUID id,
        String eventType,
        String fromStatus,
        String toStatus,
        String description,
        String actor,
        String actorRole,
        Instant occurredAt) {

    public static ReturnEventResponse from(ReturnEvent event) {
        return new ReturnEventResponse(
                event.getId(),
                event.getEventType(),
                event.getFromStatus(),
                event.getToStatus(),
                event.getDescription(),
                event.getActor(),
                event.getActorRole(),
                event.getCreatedAt());
    }
}
