CREATE TABLE events (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    venue           VARCHAR(200) NOT NULL,
    event_date      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE ticket_types (
    id                  BIGSERIAL PRIMARY KEY,
    event_id            BIGINT NOT NULL REFERENCES events(id),
    name                VARCHAR(100) NOT NULL,
    price_cents         BIGINT NOT NULL CHECK (price_cents >= 0),
    total_quantity      INTEGER NOT NULL CHECK (total_quantity >= 0),
    available_quantity  INTEGER NOT NULL CHECK (available_quantity >= 0),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT available_not_greater_than_total CHECK (available_quantity <= total_quantity)
);

CREATE INDEX idx_ticket_types_event_id ON ticket_types(event_id);

CREATE TABLE reservations (
    id                  BIGSERIAL PRIMARY KEY,
    ticket_type_id      BIGINT NOT NULL REFERENCES ticket_types(id),
    customer_id         VARCHAR(100) NOT NULL,
    quantity            INTEGER NOT NULL CHECK (quantity > 0),
    status              VARCHAR(20) NOT NULL,
    idempotency_key     VARCHAR(100) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP NOT NULL,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_reservation_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_reservations_ticket_type_id ON reservations(ticket_type_id);
CREATE INDEX idx_reservations_status_expires_at ON reservations(status, expires_at);

CREATE TABLE payments (
    id                  BIGSERIAL PRIMARY KEY,
    reservation_id      BIGINT NOT NULL REFERENCES reservations(id),
    status              VARCHAR(20) NOT NULL,
    idempotency_key     VARCHAR(100) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_payment_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_payments_reservation_id ON payments(reservation_id);
