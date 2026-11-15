package com.ticketflow.workflowservice.event;

import java.time.Instant;

public record TicketEvent(
        String eventId,
        String type,
        Long ticketId,
        Long clientId,
        String title,
        String status,
        String priority,
        Instant occurredAt
) {
}
