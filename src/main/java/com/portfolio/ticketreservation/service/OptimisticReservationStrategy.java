package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.domain.Reservation;
import com.portfolio.ticketreservation.domain.TicketType;
import com.portfolio.ticketreservation.exception.ReservationConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reserves tickets using JPA optimistic locking (the {@code @Version} column on
 * {@link TicketType}): each attempt reads the row, decrements it in memory, and
 * tries to commit. If another transaction committed a change to the same row in
 * between, Hibernate raises {@link ObjectOptimisticLockingFailureException} on
 * flush and the whole attempt is retried in a fresh transaction.
 *
 * <p>Cheap when contention is low (no row locks held while the caller thinks), but
 * wastes work and can starve under heavy contention on a single hot row — see
 * {@link PessimisticReservationStrategy} for the alternative.
 */
@Service("optimistic")
public class OptimisticReservationStrategy implements ReservationStrategy {

    private static final Logger log = LoggerFactory.getLogger(OptimisticReservationStrategy.class);

    private final TicketTypeOptimisticMutator mutator;
    private final ReservationProperties properties;

    public OptimisticReservationStrategy(TicketTypeOptimisticMutator mutator, ReservationProperties properties) {
        this.mutator = mutator;
        this.properties = properties;
    }

    @Override
    public Reservation reserve(Long ticketTypeId, String customerId, int quantity, String idempotencyKey) {
        int maxAttempts = properties.optimisticMaxRetries();
        Instant expiresAt = Instant.now().plus(properties.holdDuration());

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return mutator.attemptReserve(ticketTypeId, customerId, quantity, idempotencyKey, expiresAt);
            } catch (ObjectOptimisticLockingFailureException conflict) {
                log.debug("Optimistic lock conflict on ticket type {} (attempt {}/{})",
                        ticketTypeId, attempt, maxAttempts);
                if (attempt == maxAttempts) {
                    throw new ReservationConflictException(ticketTypeId, attempt);
                }
                backoff(attempt);
            }
        }
        throw new ReservationConflictException(ticketTypeId, maxAttempts);
    }

    private void backoff(int attempt) {
        try {
            long baseMillis = properties.optimisticRetryBackoff().toMillis();
            long jitter = ThreadLocalRandom.current().nextLong(baseMillis + 1);
            Thread.sleep(baseMillis * attempt + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
