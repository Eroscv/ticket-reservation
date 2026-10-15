package com.portfolio.ticketreservation.web.dto;

import com.portfolio.ticketreservation.domain.Reservation;
import com.portfolio.ticketreservation.domain.ReservationStatus;

import java.time.Instant;

public record ReservationResponse(
        Long id,
        Long ticketTypeId,
        String customerId,
        int quantity,
        ReservationStatus status,
        Instant createdAt,
        Instant expiresAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getTicketType().getId(),
                reservation.getCustomerId(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getExpiresAt());
    }
}
