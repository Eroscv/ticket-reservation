package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.domain.Reservation;
import com.portfolio.ticketreservation.domain.TicketType;
import com.portfolio.ticketreservation.exception.NotFoundException;
import com.portfolio.ticketreservation.repository.ReservationRepository;
import com.portfolio.ticketreservation.repository.TicketTypeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Isolated as its own Spring bean (rather than a method on
 * {@link OptimisticReservationStrategy}) so that {@code @Transactional} is applied
 * through the proxy on every call from the retry loop — a same-class call would
 * bypass the proxy and silently run without transaction boundaries, which would
 * break the retry semantics.
 */
@Component
public class TicketTypeOptimisticMutator {

    private final TicketTypeRepository ticketTypeRepository;
    private final ReservationRepository reservationRepository;

    public TicketTypeOptimisticMutator(TicketTypeRepository ticketTypeRepository,
                                        ReservationRepository reservationRepository) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation attemptReserve(Long ticketTypeId, String customerId, int quantity,
                                       String idempotencyKey, Instant expiresAt) {
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
                .orElseThrow(() -> new NotFoundException("TicketType", ticketTypeId));

        ticketType.reserve(quantity);

        Reservation reservation = new Reservation(ticketType, customerId, quantity, idempotencyKey, expiresAt);
        return reservationRepository.save(reservation);
    }
}
