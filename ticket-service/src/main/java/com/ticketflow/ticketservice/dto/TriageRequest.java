package com.ticketflow.ticketservice.dto;

import com.ticketflow.ticketservice.model.TicketCategory;
import jakarta.validation.constraints.NotNull;

public record TriageRequest(@NotNull TicketCategory category, @NotNull Boolean blocked) {
}