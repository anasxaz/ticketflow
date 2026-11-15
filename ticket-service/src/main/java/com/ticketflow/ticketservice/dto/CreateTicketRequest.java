package com.ticketflow.ticketservice.dto;

import com.ticketflow.ticketservice.model.TicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(
        @NotBlank String title,
        String description,
        @NotNull TicketCategory category,   // le client decrit un FAIT...
        @NotNull Boolean blocked            // ...et ne choisit plus la priorite
) {
}