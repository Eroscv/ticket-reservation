package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.TestcontainersConfiguration;
import com.portfolio.ticketreservation.domain.Event;
import com.portfolio.ticketreservation.domain.Payment;
import com.portfolio.ticketreservation.domain.PaymentStatus;
import com.portfolio.ticketreservation.domain.Reservation;
import com.portfolio.ticketreservation.domain.ReservationStatus;
import com.portfolio.ticketreservation.domain.TicketType;
import com.portfolio.ticketreservation.exception.ReservationExpiredException;
import com.portfolio.ticketreservation.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PaymentServiceTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void confirmMarksReservationConfirmedAndApprovesPayment() {
        Reservation reservation = reserveOneTicket("confirm-key");

        Payment payment = paymentService.confirm(reservation.getId(), "payment-key-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        Reservation reloaded = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void replayingTheSamePaymentKeyReturnsTheOriginalPayment() {
        Reservation reservation = reserveOneTicket("confirm-key-2");

        Payment first = paymentService.confirm(reservation.getId(), "payment-key-2");
        Payment second = paymentService.confirm(reservation.getId(), "payment-key-2");

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    void confirmingAnExpiredReservationFails() {
        Reservation reservation = reserveOneTicket("confirm-key-3");
        // Simulate the hold window having passed without the scheduler having run yet,
        // committing the change in its own transaction (independent of the REQUIRES_NEW
        // transaction that created the reservation, and of the one PaymentService opens next).
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                setExpiresAtInThePast(reservationRepository.findById(reservation.getId()).orElseThrow()));

        assertThatThrownBy(() -> paymentService.confirm(reservation.getId(), "payment-key-3"))
                .isInstanceOf(ReservationExpiredException.class);
    }

    private Reservation reserveOneTicket(String idempotencyKey) {
        Event event = eventService.createEvent("Payment Test Show", "Test Arena", LocalDateTime.now().plusDays(30));
        TicketType ticketType = eventService.createTicketType(event.getId(), "GA", 5_000, 10);
        return reservationService.createReservation(
                ticketType.getId(), "customer-1", 1, idempotencyKey, "optimistic");
    }

    private void setExpiresAtInThePast(Reservation reservation) {
        try {
            var field = Reservation.class.getDeclaredField("expiresAt");
            field.setAccessible(true);
            field.set(reservation, Instant.now().minusSeconds(60));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
