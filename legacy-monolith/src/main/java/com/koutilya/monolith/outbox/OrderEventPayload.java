package com.koutilya.monolith.outbox;

import java.math.BigDecimal;

/**
 * Canonical order event contract published through the outbox. Kept intentionally small and
 * versioned ({@code aggregateVersion}) so the consuming {@code orders-service} can apply events
 * idempotently and tolerate out-of-order / redelivered messages.
 */
public record OrderEventPayload(
        String orderId,
        Long customerId,
        String status,
        BigDecimal totalAmount,
        long aggregateVersion
) {
}
