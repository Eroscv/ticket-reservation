package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.domain.Reservation;
import com.portfolio.ticketreservation.exception.NotFoundException;
import com.portfolio.ticketreservation.repository.ReservationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Facade used by the web layer: resolves the idempotency key before doing any
 * work, picks the concurrency-control strategy by name ({@code "optimistic"} or
 * {@code "pessimistic"}, matching the {@code @Service} bean names), and falls back
 * to returning the existing reservation if a duplicate request loses a race on the
 * unique {@code idempotency_key} constraint.
 */
@Service
public class ReservationService {

    private final Map<String, ReservationStrategy> strategies;
    private final ReservationRepository reservationRepository;

    public ReservationService(Map<String, ReservationStrategy> strategies,
                               ReservationRepository reservationRepository) {
        this.strategies = strategies;
        this.reservationRepository = reservationRepository;
    }

    public Reservation createReservation(Long ticketTypeId, String customerId, int quantity,
                                          String idempotencyKey, String strategyName) {
        return reservationRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> reserveWithStrategy(ticketTypeId, customerId, quantity, idempotencyKey, strategyName));
    }

    private Reservation reserveWithStrategy(Long ticketTypeId, String customerId, int quantity,
                                             String idempotencyKey, String strategyName) {
        ReservationStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Unknown reservation strategy '%s', expected one of %s"
                            .formatted(strategyName, strategies.keySet()));
        }
        try {
            return strategy.reserve(ticketTypeId, customerId, quantity, idempotencyKey);
        } catch (DataIntegrityViolationException duplicateKey) {
            // Another concurrent request with the same Idempotency-Key committed first.
            return reservationRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> duplicateKey);
        }
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation", id));
    }
}
