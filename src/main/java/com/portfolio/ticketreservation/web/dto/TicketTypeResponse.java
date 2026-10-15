package com.portfolio.ticketreservation.web.dto;

import com.portfolio.ticketreservation.domain.TicketType;

public record TicketTypeResponse(
        Long id,
        Long eventId,
        String name,
        long priceCents,
        int totalQuantity,
        int availableQuantity
) {
    public static TicketTypeResponse from(TicketType ticketType) {
        return new TicketTypeResponse(
                ticketType.getId(),
                ticketType.getEvent().getId(),
                ticketType.getName(),
                ticketType.getPriceCents(),
                ticketType.getTotalQuantity(),
                ticketType.getAvailableQuantity());
    }
}
