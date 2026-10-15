package com.portfolio.ticketreservation.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String entity, Long id) {
        super("%s %d not found".formatted(entity, id));
    }
}
