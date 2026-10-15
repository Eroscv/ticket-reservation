package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.domain.Reservation;

/**
 * A way of safely decrementing {@code TicketType.availableQuantity} under
 * concurrent access and recording the resulting reservation.
 *
 * <p>Two implementations are provided to contrast the classic concurrency-control
 * techniques on the same problem: {@link OptimisticReservationStrategy} (bean name
 * {@code "optimistic"}) and {@link PessimisticReservationStrategy} (bean name
 * {@code "pessimistic"}).
 */
public interface ReservationStrategy {

    Reservation reserve(Long ticketTypeId, String customerId, int quantity, String idempotencyKey);
}
