package com.ticketflow.ticketservice.dto;

import com.ticketflow.ticketservice.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequalifyRequest(
        @NotNull Priority priority,
        @NotBlank @Size(min = 10, message = "Le motif doit faire au moins 10 caracteres") String reason
) {
}