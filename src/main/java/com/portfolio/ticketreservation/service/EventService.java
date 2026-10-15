package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.domain.Event;
import com.portfolio.ticketreservation.domain.TicketType;
import com.portfolio.ticketreservation.exception.NotFoundException;
import com.portfolio.ticketreservation.repository.EventRepository;
import com.portfolio.ticketreservation.repository.TicketTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public EventService(EventRepository eventRepository, TicketTypeRepository ticketTypeRepository) {
        this.eventRepository = eventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    @Transactional
    public Event createEvent(String name, String venue, LocalDateTime eventDate) {
        return eventRepository.save(new Event(name, venue, eventDate));
    }

    public Event getEvent(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event", id));
    }

    @Transactional
    public TicketType createTicketType(Long eventId, String name, long priceCents, int totalQuantity) {
        Event event = getEvent(eventId);
        return ticketTypeRepository.save(new TicketType(event, name, priceCents, totalQuantity));
    }

    public TicketType getTicketType(Long id) {
        return ticketTypeRepository.findById(id).orElseThrow(() -> new NotFoundException("TicketType", id));
    }
}
