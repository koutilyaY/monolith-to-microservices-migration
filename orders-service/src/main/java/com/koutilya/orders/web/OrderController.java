package com.koutilya.orders.web;

import com.koutilya.orders.domain.Order;
import com.koutilya.orders.service.OrderCommandService;
import com.koutilya.orders.service.OrderQueryService;
import com.koutilya.orders.web.dto.CreateOrderRequest;
import com.koutilya.orders.web.dto.OrderResponse;
import jakarta.validation.Valid;
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
 * Orders API served by the extracted service, honoring the monolith's original contract so the
 * gateway can route {@code /api/orders/**} here transparently.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderQueryService queryService;
    private final OrderCommandService commandService;

    public OrderController(OrderQueryService queryService, OrderCommandService commandService) {
        this.queryService = queryService;
        this.commandService = commandService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
                                                UriComponentsBuilder uriBuilder) {
        Order order = commandService.createOrder(request.customerId(), request.totalAmount());
        URI location = uriBuilder.path("/api/orders/{id}").buildAndExpand(order.getId()).toUri();
        return ResponseEntity.created(location).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable String id) {
        return OrderResponse.from(queryService.getOrder(id));
    }

    @GetMapping
    public List<OrderResponse> listByCustomer(@RequestParam Long customerId) {
        return queryService.listByCustomer(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }
}
