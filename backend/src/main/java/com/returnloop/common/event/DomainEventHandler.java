package com.returnloop.common.event;

/**
 * Consumer of a domain event. Implementations must be idempotent: the same
 * {@link DomainEvent#eventId()} may be delivered more than once.
 */
public interface DomainEventHandler {

    /** Event type this handler reacts to, e.g. {@code inspection.completed}. */
    String eventType();

    void handle(DomainEvent event);
}
