package com.portfolio.ticketreservation.repository;

import com.portfolio.ticketreservation.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
