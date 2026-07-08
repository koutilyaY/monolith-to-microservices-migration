package com.koutilya.monolith.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MirrorOrderRequest(
        @NotBlank String id,
        @NotNull Long customerId,
        @NotBlank String status,
        @NotNull BigDecimal totalAmount,
        long version
) {
}
