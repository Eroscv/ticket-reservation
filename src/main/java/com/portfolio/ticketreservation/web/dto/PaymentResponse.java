package com.portfolio.ticketreservation.web.dto;

import com.portfolio.ticketreservation.domain.Payment;
import com.portfolio.ticketreservation.domain.PaymentStatus;

import java.time.Instant;

public record PaymentResponse(Long id, Long reservationId, PaymentStatus status, Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(), payment.getReservation().getId(), payment.getStatus(), payment.getCreatedAt());
    }
}
