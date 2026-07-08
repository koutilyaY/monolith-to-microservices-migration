package com.koutilya.orders.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotNull Long customerId,
        @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal totalAmount
) {
}
