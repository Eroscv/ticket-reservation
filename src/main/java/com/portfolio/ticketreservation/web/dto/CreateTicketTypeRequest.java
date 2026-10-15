package com.portfolio.ticketreservation.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateTicketTypeRequest(
        @NotBlank String name,
        @Min(0) long priceCents,
        @Min(1) int totalQuantity
) {
}
