package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.domain.Payment;
import com.portfolio.ticketreservation.domain.PaymentStatus;
import com.portfolio.ticketreservation.domain.Reservation;
import com.portfolio.ticketreservation.domain.ReservationStatus;
import com.portfolio.ticketreservation.exception.NotFoundException;
import com.portfolio.ticketreservation.exception.ReservationExpiredException;
import com.portfolio.ticketreservation.repository.PaymentRepository;
import com.portfolio.ticketreservation.repository.ReservationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Its own bean (not a method on {@link PaymentService}) so {@code @Transactional}
 * goes through the Spring proxy instead of being silently skipped by a
 * same-class call — see the equivalent note on
 * {@link TicketTypeOptimisticMutator}.
 */
@Component
public class PaymentConfirmer {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    public PaymentConfirmer(ReservationRepository reservationRepository, PaymentRepository paymentRepository) {
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment confirm(Long reservationId, String idempotencyKey) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation", reservationId));

        if (reservation.isExpiredAsOf(Instant.now())) {
            reservation.expire();
            throw new ReservationExpiredException(reservationId);
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "Reservation %d is %s, cannot confirm payment".formatted(reservationId, reservation.getStatus()));
        }

        reservation.confirm();
        Payment payment = new Payment(reservation, PaymentStatus.APPROVED, idempotencyKey);
        return paymentRepository.save(payment);
    }
}
