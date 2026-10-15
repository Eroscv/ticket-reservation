package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.domain.Reservation;
import com.portfolio.ticketreservation.domain.ReservationStatus;
import com.portfolio.ticketreservation.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Releases seats held by reservations that were never confirmed in time.
 * Each expired reservation is handled in its own transaction by
 * {@link ReservationReleaser}, which takes the same pessimistic row lock on the
 * ticket type as {@link PessimisticReservationStrategy} to safely add the seats
 * back to {@code availableQuantity} alongside any in-flight reservation attempts.
 */
@Component
public class ReservationExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationScheduler.class);

    private final ReservationRepository reservationRepository;
    private final ReservationReleaser reservationReleaser;

    public ReservationExpirationScheduler(ReservationRepository reservationRepository,
                                           ReservationReleaser reservationReleaser) {
        this.reservationRepository = reservationRepository;
        this.reservationReleaser = reservationReleaser;
    }

    @Scheduled(fixedDelayString = "${reservation.expiration-check-interval-ms:30000}")
    public void releaseExpiredReservations() {
        List<Reservation> expired =
                reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, Instant.now());

        for (Reservation reservation : expired) {
            reservationReleaser.releaseOne(reservation.getId());
        }
        if (!expired.isEmpty()) {
            log.info("Released {} expired reservation(s)", expired.size());
        }
    }
}
