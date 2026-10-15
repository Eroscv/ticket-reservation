package com.portfolio.ticketreservation.web;

import com.portfolio.ticketreservation.service.EventService;
import com.portfolio.ticketreservation.web.dto.CreateEventRequest;
import com.portfolio.ticketreservation.web.dto.CreateTicketTypeRequest;
import com.portfolio.ticketreservation.web.dto.EventResponse;
import com.portfolio.ticketreservation.web.dto.TicketTypeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        var event = eventService.createEvent(request.name(), request.venue(), request.eventDate());
        return EventResponse.from(event);
    }

    @PostMapping("/{eventId}/ticket-types")
    public ResponseEntity<TicketTypeResponse> createTicketType(@PathVariable Long eventId,
                                                                 @Valid @RequestBody CreateTicketTypeRequest request) {
        var ticketType = eventService.createTicketType(
                eventId, request.name(), request.priceCents(), request.totalQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketTypeResponse.from(ticketType));
    }

    @GetMapping("/ticket-types/{ticketTypeId}")
    public TicketTypeResponse getTicketType(@PathVariable Long ticketTypeId) {
        return TicketTypeResponse.from(eventService.getTicketType(ticketTypeId));
    }
}
