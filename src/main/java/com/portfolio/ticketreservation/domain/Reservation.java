package com.portfolio.ticketreservation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_type_id")
    private TicketType ticketType;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Version
    private long version;

    protected Reservation() {
    }

    public Reservation(TicketType ticketType, String customerId, int quantity,
                        String idempotencyKey, Instant expiresAt) {
        this.ticketType = ticketType;
        this.customerId = customerId;
        this.quantity = quantity;
        this.status = ReservationStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm reservation %d in status %s".formatted(id, status));
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    public void expire() {
        if (status == ReservationStatus.PENDING) {
            this.status = ReservationStatus.EXPIRED;
        }
    }

    public boolean isExpiredAsOf(Instant now) {
        return status == ReservationStatus.PENDING && expiresAt.isBefore(now);
    }

    public Long getId() {
        return id;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public String getCustomerId() {
        return customerId;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
