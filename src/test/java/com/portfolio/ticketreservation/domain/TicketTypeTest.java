package com.portfolio.ticketreservation.domain;

import com.portfolio.ticketreservation.exception.InsufficientTicketsException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTypeTest {

    private final Event event = new Event("Show", "Arena", LocalDateTime.now().plusDays(1));

    @Test
    void reserveDecrementsAvailableQuantity() {
        TicketType ticketType = new TicketType(event, "GA", 1_000, 10);

        ticketType.reserve(3);

        assertThat(ticketType.getAvailableQuantity()).isEqualTo(7);
        assertThat(ticketType.getTotalQuantity()).isEqualTo(10);
    }

    @Test
    void reserveRejectsQuantityAboveAvailable() {
        TicketType ticketType = new TicketType(event, "GA", 1_000, 5);
        ticketType.reserve(5);

        assertThatThrownBy(() -> ticketType.reserve(1))
                .isInstanceOf(InsufficientTicketsException.class);

        assertThat(ticketType.getAvailableQuantity()).isZero();
    }

    @Test
    void releaseNeverExceedsTotalQuantity() {
        TicketType ticketType = new TicketType(event, "GA", 1_000, 5);
        ticketType.reserve(2);

        ticketType.release(10);

        assertThat(ticketType.getAvailableQuantity()).isEqualTo(5);
    }
}
