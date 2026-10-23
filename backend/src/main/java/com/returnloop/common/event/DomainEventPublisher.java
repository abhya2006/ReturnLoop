package com.returnloop.common.event;

/**
 * Abstraction over the event transport. The default implementation dispatches
 * synchronously in-process; a Kafka-backed implementation is enabled with
 * {@code returnloop.messaging.kafka.enabled=true}.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
