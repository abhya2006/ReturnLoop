package com.returnloop.common.event;

import com.returnloop.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Turns domain events into in-app notifications.
 */
@Component
@RequiredArgsConstructor
public class NotificationEventHandler implements DomainEventHandler {

    private final NotificationService notificationService;

    @Override
    public String eventType() {
        return "*";
    }

    @Override
    public void handle(DomainEvent event) {
        Map<String, Object> payload = event.payload();
        switch (event.type()) {
            case "return.created" -> notify(event, "New return request",
                    "Return " + payload.getOrDefault("returnNumber", "") + " was created with "
                            + payload.getOrDefault("itemCount", 0) + " item(s)",
                    "RETURNS", "INFO", "/returns");
            case "pickup.scheduled" -> notify(event, "Pickup scheduled",
                    "Pickup " + payload.getOrDefault("pickupNumber", "") + " was scheduled for "
                            + payload.getOrDefault("date", ""),
                    "LOGISTICS", "INFO", "/pickups");
            case "inspection.completed" -> notify(event, "Inspection completed",
                    "Item " + payload.getOrDefault("sku", "") + " was inspected with result "
                            + payload.getOrDefault("status", ""),
                    "INSPECTIONS", "INFO", "/inspections");
            case "recovery.recommended" -> notify(event, "Recovery recommendation generated",
                    "Recovery options were calculated for " + payload.getOrDefault("itemCount", 0)
                            + " item(s) using the " + payload.getOrDefault("strategy", "") + " strategy",
                    "RECOVERY", "INFO", "/recovery");
            case "partner.job-assigned" -> notify(event, "Partner accepted a job",
                    "A job was assigned to " + payload.getOrDefault("partner", ""),
                    "PARTNERS", "INFO", "/partner-jobs");
            case "partner.job-status-changed" -> notify(event, "Recovery job updated",
                    "A recovery job moved from " + payload.getOrDefault("from", "") + " to "
                            + payload.getOrDefault("to", ""),
                    "PARTNERS", "INFO", "/partner-jobs");
            case "route.optimized" -> notify(event, "Routes recalculated",
                    payload.getOrDefault("routes", 0) + " route(s) were planned for "
                            + payload.getOrDefault("date", ""),
                    "LOGISTICS", "INFO", "/routes");
            default -> {
            }
        }
    }

    private void notify(DomainEvent event, String title, String body, String category, String severity,
                        String link) {
        if (event.organizationId() == null) {
            return;
        }
        notificationService.notifyOrganization(event.organizationId(), title, body, category, severity, link,
                event.aggregateType(), event.aggregateId());
    }
}
