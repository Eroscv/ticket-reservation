package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.domain.Payment;
import com.portfolio.ticketreservation.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Confirms a {@link com.portfolio.ticketreservation.domain.Reservation} and
 * records a {@link Payment}, idempotently on the caller-supplied
 * {@code Idempotency-Key}: replaying the same key after a timeout or client retry
 * returns the original payment instead of charging twice.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentConfirmer paymentConfirmer;

    public PaymentService(PaymentRepository paymentRepository, PaymentConfirmer paymentConfirmer) {
        this.paymentRepository = paymentRepository;
        this.paymentConfirmer = paymentConfirmer;
    }

    public Payment confirm(Long reservationId, String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> confirmGuardingDuplicateKey(reservationId, idempotencyKey));
    }

    private Payment confirmGuardingDuplicateKey(Long reservationId, String idempotencyKey) {
        try {
            return paymentConfirmer.confirm(reservationId, idempotencyKey);
        } catch (DataIntegrityViolationException duplicateKey) {
            // Another concurrent request with the same Idempotency-Key committed first.
            return paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> duplicateKey);
        }
    }
}
