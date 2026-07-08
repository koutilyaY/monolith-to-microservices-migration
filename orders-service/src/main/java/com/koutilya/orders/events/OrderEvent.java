package com.koutilya.orders.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Consumer-side view of the order event contract published by the monolith outbox. Deliberately
 * lenient ({@code @JsonIgnoreProperties}) so the producer can add fields without breaking us.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderEvent(
        String orderId,
        Long customerId,
        String status,
        BigDecimal totalAmount,
        long aggregateVersion
) {
}
