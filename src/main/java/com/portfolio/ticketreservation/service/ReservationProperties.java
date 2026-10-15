package com.portfolio.ticketreservation.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "reservation")
public record ReservationProperties(
        Duration holdDuration,
        int optimisticMaxRetries,
        Duration optimisticRetryBackoff
) {
    public ReservationProperties {
        if (holdDuration == null) {
            holdDuration = Duration.ofMinutes(15);
        }
        if (optimisticMaxRetries <= 0) {
            optimisticMaxRetries = 5;
        }
        if (optimisticRetryBackoff == null) {
            optimisticRetryBackoff = Duration.ofMillis(20);
        }
    }
}
