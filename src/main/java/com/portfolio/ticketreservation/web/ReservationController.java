package com.portfolio.ticketreservation.web;

import com.portfolio.ticketreservation.service.ReservationService;
import com.portfolio.ticketreservation.web.dto.CreateReservationRequest;
import com.portfolio.ticketreservation.web.dto.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * {@code strategy} picks the concurrency-control technique and defaults to
     * {@code optimistic}; pass {@code pessimistic} to compare the alternative.
     * The {@code Idempotency-Key} header makes retries of this call safe:
     * replaying the same key returns the original reservation instead of booking
     * twice.
     */
    @PostMapping("/ticket-types/{ticketTypeId}/reservations")
    public ResponseEntity<ReservationResponse> createReservation(
            @PathVariable Long ticketTypeId,
            @Valid @RequestBody CreateReservationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestParam(defaultValue = "optimistic") String strategy) {

        var reservation = reservationService.createReservation(
                ticketTypeId, request.customerId(), request.quantity(), idempotencyKey, strategy);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(reservation));
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse getReservation(@PathVariable Long id) {
        return ReservationResponse.from(reservationService.getReservation(id));
    }
}
