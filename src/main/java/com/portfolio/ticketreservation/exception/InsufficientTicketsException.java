package com.portfolio.ticketreservation.exception;

public class InsufficientTicketsException extends RuntimeException {

    public InsufficientTicketsException(Long ticketTypeId, int requested, int available) {
        super("Not enough tickets for ticket type %d: requested %d, available %d"
                .formatted(ticketTypeId, requested, available));
    }
}
