package com.koutilya.monolith.web;

import com.koutilya.monolith.domain.OrderEntity;
import com.koutilya.monolith.service.OrderService;
import com.koutilya.monolith.web.dto.CreateOrderRequest;
import com.koutilya.monolith.web.dto.OrderResponse;
import com.koutilya.monolith.web.dto.TransitionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * The original Orders REST API. The {@code orders-service} implements the same contract so the
 * gateway can route {@code /api/orders/**} to either backend during the strangle.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
                                                UriComponentsBuilder uriBuilder) {
        OrderEntity order = orderService.createOrder(request.customerId(), request.totalAmount());
        URI location = uriBuilder.path("/api/orders/{id}").buildAndExpand(order.getId()).toUri();
        return ResponseEntity.created(location).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable String id) {
        return OrderResponse.from(orderService.getOrder(id));
    }

    @GetMapping
    public List<OrderResponse> listByCustomer(@RequestParam Long customerId) {
        return orderService.listByCustomer(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @PostMapping("/{id}/transition")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.OK)
    public OrderResponse transition(@PathVariable String id,
                                    @Valid @RequestBody TransitionRequest request) {
        return OrderResponse.from(orderService.transition(id, request.status()));
    }
}
