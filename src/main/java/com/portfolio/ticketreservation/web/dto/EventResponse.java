package com.portfolio.ticketreservation.web.dto;

import com.portfolio.ticketreservation.domain.Event;

import java.time.LocalDateTime;

public record EventResponse(Long id, String name, String venue, LocalDateTime eventDate) {

    public static EventResponse from(Event event) {
        return new EventResponse(event.getId(), event.getName(), event.getVenue(), event.getEventDate());
    }
}
