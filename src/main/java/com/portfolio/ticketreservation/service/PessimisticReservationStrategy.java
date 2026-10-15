package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.domain.Reservation;
import com.portfolio.ticketreservation.domain.TicketType;
import com.portfolio.ticketreservation.exception.NotFoundException;
import com.portfolio.ticketreservation.repository.ReservationRepository;
import com.portfolio.ticketreservation.repository.TicketTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Reserves tickets by taking a {@code SELECT ... FOR UPDATE} row lock
 * ({@link TicketTypeRepository#findByIdForUpdate}) on the {@link TicketType} before
 * reading its {@code availableQuantity}. Concurrent requests for the same ticket
 * type simply queue up on the database row lock and are each served in turn — no
 * wasted retries, but a slow transaction holds up every other reservation for that
 * ticket type for its whole duration. Good when contention on a single row is
 * expected to be high.
 */
@Service("pessimistic")
public class PessimisticReservationStrategy implements ReservationStrategy {

    private final TicketTypeRepository ticketTypeRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationProperties properties;

    public PessimisticReservationStrategy(TicketTypeRepository ticketTypeRepository,
                                           ReservationRepository reservationRepository,
                                           ReservationProperties properties) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.reservationRepository = reservationRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public Reservation reserve(Long ticketTypeId, String customerId, int quantity, String idempotencyKey) {
        TicketType ticketType = ticketTypeRepository.findByIdForUpdate(ticketTypeId)
                .orElseThrow(() -> new NotFoundException("TicketType", ticketTypeId));

        ticketType.reserve(quantity);

        Instant expiresAt = Instant.now().plus(properties.holdDuration());
        Reservation reservation = new Reservation(ticketType, customerId, quantity, idempotencyKey, expiresAt);
        return reservationRepository.save(reservation);
    }
}
