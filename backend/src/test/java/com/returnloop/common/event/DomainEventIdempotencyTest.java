package com.returnloop.common.event;

import com.returnloop.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The in-process publisher must tolerate a redelivered event: the consumer
 * decides what to do with it, but a duplicate must not produce a second
 * notification.
 */
class DomainEventIdempotencyTest {

    @Test
    @DisplayName("a redelivered event is recognised and skipped")
    void duplicateEventsAreSkipped() {
        NotificationService notificationService = mock(NotificationService.class);
        DomainEventHandler handler = new NotificationEventHandler(notificationService);

        InProcessDomainEventPublisher publisher = new InProcessDomainEventPublisher(java.util.List.of(handler));

        DomainEvent event = DomainEvent.of("return.created", UUID.randomUUID(), "ReturnRequest",
                UUID.randomUUID(), "tester", Map.of("returnNumber", "RET-1", "itemCount", 1));

        publisher.publish(event);
        publisher.publish(event);

        // The redelivery must not produce a second notification.
        verify(notificationService, times(1)).notifyOrganization(any(), anyString(), anyString(),
                anyString(), anyString(), any(), anyString(), any());
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    @DisplayName("handlers only receive the event types they subscribe to")
    void handlersAreFilteredByType() {
        NotificationService notificationService = mock(NotificationService.class);
        DomainEventHandler handler = new NotificationEventHandler(notificationService);

        InProcessDomainEventPublisher publisher = new InProcessDomainEventPublisher(java.util.List.of(handler));

        publisher.publish(DomainEvent.of("unrelated.event", UUID.randomUUID(), "ReturnRequest",
                UUID.randomUUID(), "tester", Map.of()));

        verify(notificationService, never()).notifyOrganization(any(), anyString(), anyString(),
                anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    @DisplayName("a return.created event produces a notification")
    void createsNotification() {
        NotificationService notificationService = mock(NotificationService.class);
        DomainEventHandler handler = new NotificationEventHandler(notificationService);

        handler.handle(DomainEvent.of("return.created", UUID.randomUUID(), "ReturnRequest",
                UUID.randomUUID(), "tester", Map.of("returnNumber", "RET-1", "itemCount", 2)));

        verify(notificationService, times(1)).notifyOrganization(any(), anyString(), anyString(),
                anyString(), anyString(), any(), anyString(), any());
        Mockito.reset(notificationService);
    }
}
