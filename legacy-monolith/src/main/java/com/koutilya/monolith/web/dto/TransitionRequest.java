package com.koutilya.monolith.web.dto;

import jakarta.validation.constraints.NotNull;

import com.koutilya.monolith.domain.OrderStatus;

public record TransitionRequest(
        @NotNull OrderStatus status
) {
}
