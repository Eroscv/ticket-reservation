package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.domain.Reservation;
import com.portfolio.ticketreservation.domain.ReservationStatus;
import com.portfolio.ticketreservation.repository.ReservationRepository;
import com.portfolio.ticketreservation.repository.TicketTypeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Its own bean so {@code @Transactional} applies on every call from the
 * scheduler's loop — see the equivalent note on {@link TicketTypeOptimisticMutator}.
 */
@Component
public class ReservationReleaser {

    private final ReservationRepository reservationRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public ReservationReleaser(ReservationRepository reservationRepository,
                                TicketTypeRepository ticketTypeRepository) {
        this.reservationRepository = reservationRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    @Transactional
    public void releaseOne(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null || reservation.getStatus() != ReservationStatus.PENDING) {
            return; // already confirmed/cancelled/expired by another path - nothing to do
        }

        ticketTypeRepository.findByIdForUpdate(reservation.getTicketType().getId())
                .ifPresent(ticketType -> ticketType.release(reservation.getQuantity()));

        reservation.expire();
    }
}
