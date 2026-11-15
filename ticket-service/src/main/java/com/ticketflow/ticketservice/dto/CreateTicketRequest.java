package com.ticketflow.ticketservice.dto;

import com.ticketflow.ticketservice.model.TicketCategory;
import jakarta.validation.constraints.NotBlank;

public record CreateTicketRequest(
        @NotBlank String title,
        String description,
        TicketCategory category,   // optionnel : formulaire (V1)...
        Boolean blocked            // ...sinon le LLM lit la description (V2)
) {
}