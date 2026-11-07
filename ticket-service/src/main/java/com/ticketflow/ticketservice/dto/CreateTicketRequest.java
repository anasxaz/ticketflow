package com.ticketflow.ticketservice.dto;

import com.ticketflow.ticketservice.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(
        @NotBlank String title,
        String description,
        @NotNull Priority priority
) {
}
