package com.ticketflow.ticketservice.event;

import java.time.Instant;

public record TicketEvent(
        String eventId,  //pour un UUID unique qui va t être utilisé pour l'idempotence
        String type,   //pour dire que TICKET_CREATED ou STATUS_CHANGED
        Long ticketId,
        Long clientId,
        String title,
        String status,
        String priority,
        String description,
        Boolean needsReview,
        Instant occurredAt
) {
}
