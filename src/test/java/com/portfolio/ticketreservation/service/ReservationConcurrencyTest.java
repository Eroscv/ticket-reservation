package com.portfolio.ticketreservation.service;

import com.portfolio.ticketreservation.TestcontainersConfiguration;
import com.portfolio.ticketreservation.domain.Event;
import com.portfolio.ticketreservation.domain.ReservationStatus;
import com.portfolio.ticketreservation.domain.TicketType;
import com.portfolio.ticketreservation.exception.InsufficientTicketsException;
import com.portfolio.ticketreservation.exception.ReservationConflictException;
import com.portfolio.ticketreservation.repository.ReservationRepository;
import com.portfolio.ticketreservation.repository.TicketTypeRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The actual proof that neither strategy oversells: {@code CONCURRENT_CUSTOMERS}
 * threads race to each reserve one seat out of a much smaller pool. Whichever
 * strategy is under test, exactly {@code totalQuantity} reservations must
 * succeed, the rest must fail with a well-defined exception, and the ticket
 * type's counter must land on exactly zero - never negative, never left with
 * phantom availability.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=25"
})
class ReservationConcurrencyTest {

    private static final int TOTAL_SEATS = 15;
    private static final int CONCURRENT_CUSTOMERS = 60;

    @Autowired
    private EventService eventService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @ParameterizedTest
    @ValueSource(strings = {"optimistic", "pessimistic"})
    void exactlyTotalQuantitySucceedsUnderConcurrentDemand(String strategy) throws Exception {
        Event event = eventService.createEvent(
                "Concurrency Test Show", "Test Arena", LocalDateTime.now().plusDays(30));
        TicketType ticketType = eventService.createTicketType(
                event.getId(), "GA-" + strategy, 5_000, TOTAL_SEATS);
        Long ticketTypeId = ticketType.getId();

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger expectedFailures = new AtomicInteger();

        List<Callable<Void>> tasks = IntStream.range(0, CONCURRENT_CUSTOMERS)
                .<Callable<Void>>mapToObj(i -> () -> {
                    try {
                        reservationService.createReservation(
                                ticketTypeId, "customer-" + i, 1, strategy + "-key-" + i, strategy);
                        successes.incrementAndGet();
                    } catch (InsufficientTicketsException | ReservationConflictException expected) {
                        expectedFailures.incrementAndGet();
                    }
                    return null;
                })
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_CUSTOMERS);
        try {
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get(60, TimeUnit.SECONDS); // surface any unexpected exception
            }
        } finally {
            executor.shutdown();
        }

        assertThat(successes.get()).isEqualTo(TOTAL_SEATS);
        assertThat(expectedFailures.get()).isEqualTo(CONCURRENT_CUSTOMERS - TOTAL_SEATS);

        TicketType reloaded = ticketTypeRepository.findById(ticketTypeId).orElseThrow();
        assertThat(reloaded.getAvailableQuantity()).isZero();

        long confirmedPending = reservationRepository.findAll().stream()
                .filter(r -> r.getTicketType().getId().equals(ticketTypeId))
                .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                .count();
        assertThat(confirmedPending).isEqualTo(TOTAL_SEATS);
    }
}
