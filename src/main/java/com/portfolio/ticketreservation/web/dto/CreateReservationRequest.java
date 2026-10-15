package com.portfolio.ticketreservation.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateReservationRequest(
        @NotBlank String customerId,
        @Min(1) int quantity
) {
}
