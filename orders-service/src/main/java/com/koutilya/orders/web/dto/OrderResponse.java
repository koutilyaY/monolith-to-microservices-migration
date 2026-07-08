package com.koutilya.orders.web.dto;

import com.koutilya.orders.domain.Order;

import java.math.BigDecimal;

/**
 * Wire contract for an order. MUST stay byte-compatible with the monolith's {@code OrderResponse};
 * that compatibility is what the Spring Cloud Contract tests guard.
 */
public record OrderResponse(
        String id,
        Long customerId,
        String status,
        BigDecimal totalAmount,
        long version
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getVersion());
    }
}
