package com.returnloop.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to Kafka.
 *
 * <p>Enabled with {@code returnloop.messaging.kafka.enabled=true}. The
 * in-process publisher is used otherwise, so the application runs without a
 * broker during local development.
 */
@Component
@ConditionalOnProperty(name = "returnloop.messaging.kafka.enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final com.returnloop.common.config.ReturnLoopProperties properties;

    @Override
    public void publish(DomainEvent event) {
        try {
            kafkaTemplate.send(properties.getMessaging().getTopic(), event.aggregateId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event {} to Kafka: {}", event.eventId(), ex.getMessage());
                        }
                    });
        } catch (RuntimeException ex) {
            log.error("Failed to publish event {} to Kafka: {}", event.eventId(), ex.getMessage());
        }
    }
}
