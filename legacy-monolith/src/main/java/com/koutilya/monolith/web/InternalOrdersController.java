package com.koutilya.monolith.web;

import com.koutilya.monolith.domain.OrderStatus;
import com.koutilya.monolith.service.OrderService;
import com.koutilya.monolith.web.dto.MirrorOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal dual-write sink. During the canary WRITE phase, orders-service mirrors the orders it
 * creates back into the monolith through this endpoint so the not-yet-extracted parts of the
 * system keep a complete view and rollback loses nothing. The upsert is idempotent by version.
 */
@RestController
@RequestMapping("/internal/orders")
public class InternalOrdersController {

    private final OrderService orderService;

    public InternalOrdersController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/mirror")
    public ResponseEntity<Void> mirror(@Valid @RequestBody MirrorOrderRequest request) {
        orderService.mirrorUpsert(
                request.id(),
                request.customerId(),
                OrderStatus.valueOf(request.status()),
                request.totalAmount(),
                request.version());
        return ResponseEntity.accepted().build();
    }
}
