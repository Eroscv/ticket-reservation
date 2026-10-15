package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.TestcontainersConfiguration;
import com.portfolio.ticketreservation.domain.Event;
import com.portfolio.ticketreservation.domain.Reservation;
import com.portfolio.ticketreservation.domain.TicketType;
import com.portfolio.ticketreservation.repository.TicketTypeRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replaying the exact same {@code Idempotency-Key} - whether sequentially (a
 * client retry after a timeout) or concurrently (two in-flight copies of the
 * same retry) - must return the same {@link Reservation} and only ever
 * decrement the seat count once.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReservationIdempotencyTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @ParameterizedTest
    @ValueSource(strings = {"optimistic", "pessimistic"})
    void sequentialReplayReturnsTheSameReservation(String strategy) {
        TicketType ticketType = newTicketType(strategy);
        String idempotencyKey = "replay-" + strategy;

        Reservation first = reservationService.createReservation(
                ticketType.getId(), "customer-1", 2, idempotencyKey, strategy);
        Reservation second = reservationService.createReservation(
                ticketType.getId(), "customer-1", 2, idempotencyKey, strategy);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(ticketTypeRepository.findById(ticketType.getId()).orElseThrow().getAvailableQuantity())
                .isEqualTo(8); // decremented once, not twice
    }

    @ParameterizedTest
    @ValueSource(strings = {"optimistic", "pessimistic"})
    void concurrentReplayWithSameKeyDecrementsOnlyOnce(String strategy) throws Exception {
        TicketType ticketType = newTicketType(strategy);
        String idempotencyKey = "concurrent-replay-" + strategy;

        List<Callable<Reservation>> tasks = IntStream.range(0, 10)
                .<Callable<Reservation>>mapToObj(i -> () -> reservationService.createReservation(
                        ticketType.getId(), "customer-1", 2, idempotencyKey, strategy))
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Long> reservationIds;
        try {
            List<Future<Reservation>> futures = executor.invokeAll(tasks);
            reservationIds = futures.stream().map(f -> {
                try {
                    return f.get().getId();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toList();
        } finally {
            executor.shutdown();
        }

        assertThat(reservationIds.stream().distinct().count()).isEqualTo(1);
        assertThat(ticketTypeRepository.findById(ticketType.getId()).orElseThrow().getAvailableQuantity())
                .isEqualTo(8);
    }

    private TicketType newTicketType(String strategy) {
        Event event = eventService.createEvent(
                "Idempotency Test Show", "Test Arena", LocalDateTime.now().plusDays(30));
        return eventService.createTicketType(event.getId(), "GA-" + strategy, 5_000, 10);
    }
}
