package com.koutilya.orders.service;

import com.koutilya.orders.domain.Order;
import com.koutilya.orders.domain.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Handles writes the gateway routes to the new service (the canary WRITE phase). The local write
 * is the source of truth and commits first. The dual-write mirror to the monolith is triggered
 * only AFTER commit (via {@link OrderMirrorListener} listening for {@link OrderCreatedLocally})
 * so that (a) a mirror failure can never roll back the customer's order and (b) no DB connection
 * is held open across a network call.
 */
@Service
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher events;

    public OrderCommandService(OrderRepository orderRepository, ApplicationEventPublisher events) {
        this.orderRepository = orderRepository;
        this.events = events;
    }

    @Transactional
    public Order createOrder(Long customerId, BigDecimal totalAmount) {
        Order order = orderRepository.save(new Order(
                UUID.randomUUID().toString(),
                customerId,
                totalAmount,
                "PLACED",
                1L));
        events.publishEvent(new OrderCreatedLocally(order.getId()));
        return order;
    }
}
