package com.portfolio.ticketreservation.exception;

/** Thrown when the optimistic strategy exhausts its retry budget under contention. */
public class ReservationConflictException extends RuntimeException {

    public ReservationConflictException(Long ticketTypeId, int attempts) {
        super("Could not reserve ticket type %d after %d attempts due to concurrent updates"
                .formatted(ticketTypeId, attempts));
    }
}
