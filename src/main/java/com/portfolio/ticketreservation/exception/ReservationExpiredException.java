package com.portfolio.ticketreservation.exception;

public class ReservationExpiredException extends RuntimeException {

    public ReservationExpiredException(Long reservationId) {
        super("Reservation %d has expired and can no longer be confirmed".formatted(reservationId));
    }
}
