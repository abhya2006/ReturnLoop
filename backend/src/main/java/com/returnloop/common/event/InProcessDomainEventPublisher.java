package com.returnloop.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatches domain events to their handlers in-process.
 *
 * <p>Handlers are invoked after the originating transaction commits, so a
 * handler never observes partially written state. The same event may be
 * delivered more than once; handlers are responsible for idempotency.
 */
@Component
@ConditionalOnProperty(name = "returnloop.messaging.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class InProcessDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InProcessDomainEventPublisher.class);

    private final List<DomainEventHandler> handlers;
    private final Map<String, String> processedEvents = new ConcurrentHashMap<>();

    public InProcessDomainEventPublisher(@Lazy List<DomainEventHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event.eventId() != null && !firstDelivery(event.eventId())) {
            log.debug("Skipping duplicate delivery of event {}", event.eventId());
            return;
        }
        for (DomainEventHandler handler : handlers) {
            String type = handler.eventType();
            if (!"*".equals(type) && !type.equals(event.type())) {
                continue;
            }
            try {
                handler.handle(event);
            } catch (RuntimeException ex) {
                log.error("Handler {} failed for event {} ({})", handler.getClass().getSimpleName(),
                        event.type(), event.eventId(), ex);
            }
        }
    }

    /**
     * Records that an event has been handled, so a replayed event can be skipped.
     *
     * @return true when this is the first time the event id has been seen
     */
    protected boolean firstDelivery(UUID eventId) {
        return processedEvents.putIfAbsent(eventId.toString(), "1") == null;
    }
}
