package com.portfolio.ticketreservation.domain;

import com.portfolio.ticketreservation.exception.InsufficientTicketsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * The contended resource: available_quantity is mutated by concurrent reservation
 * attempts. The {@code version} column backs JPA optimistic locking; the pessimistic
 * reservation strategy instead locks this row with SELECT ... FOR UPDATE.
 */
@Entity
@Table(name = "ticket_types")
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Version
    private long version;

    protected TicketType() {
    }

    public TicketType(Event event, String name, long priceCents, int totalQuantity) {
        this.event = event;
        this.name = name;
        this.priceCents = priceCents;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = totalQuantity;
    }

    /** Throws if there is not enough stock; never lets availableQuantity go negative. */
    public void reserve(int quantity) {
        if (quantity > availableQuantity) {
            throw new InsufficientTicketsException(id, quantity, availableQuantity);
        }
        availableQuantity -= quantity;
    }

    public void release(int quantity) {
        availableQuantity = Math.min(totalQuantity, availableQuantity + quantity);
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public String getName() {
        return name;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public long getVersion() {
        return version;
    }
}
