package com.portfolio.ticketreservation.web;

import com.portfolio.ticketreservation.service.PaymentService;
import com.portfolio.ticketreservation.web.dto.PaymentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable Long id,
                                                            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        var payment = paymentService.confirm(id, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }
}
