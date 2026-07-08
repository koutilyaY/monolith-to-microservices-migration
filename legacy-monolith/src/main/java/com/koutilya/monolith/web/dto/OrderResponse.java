package com.koutilya.monolith.web.dto;

import com.koutilya.monolith.domain.OrderEntity;

import java.math.BigDecimal;

/**
 * Wire contract for an order. The {@code orders-service} reproduces this exact shape so the
 * gateway can route the same path to either backend transparently.
 */
public record OrderResponse(
        String id,
        Long customerId,
        String status,
        BigDecimal totalAmount,
        long version
) {
    public static OrderResponse from(OrderEntity order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getAggregateVersion());
    }
}
