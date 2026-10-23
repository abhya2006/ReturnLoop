package com.returnloop.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes domain events from Kafka.
 *
 * <p>The consumer is idempotent: a redelivered event is recognised by its
 * {@code eventId} and ignored. Failures are retried with backoff and then
 * routed to the dead-letter topic.
 */
@Component
@ConditionalOnProperty(name = "returnloop.messaging.kafka.enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class KafkaDomainEventConsumer {

    private final DomainEventHandler notificationHandler;
    private final ObjectMapper objectMapper;
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();

    @KafkaListener(topics = "${returnloop.messaging.topic:returnloop.events}",
            groupId = "returnloop-consumers")
    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = ".DLT")
    public void consume(DomainEvent event) {
        if (event == null || event.eventId() == null) {
            return;
        }
        if (!processedEvents.add(event.eventId().toString())) {
            log.debug("Skipping duplicate delivery of event {}", event.eventId());
            return;
        }
        notificationHandler.handle(event);
    }
}
