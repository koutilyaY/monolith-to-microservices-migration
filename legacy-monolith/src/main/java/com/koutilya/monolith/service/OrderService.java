package com.koutilya.monolith.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koutilya.monolith.domain.OrderEntity;
import com.koutilya.monolith.domain.OrderRepository;
import com.koutilya.monolith.domain.OrderStatus;
import com.koutilya.monolith.outbox.OrderEventPayload;
import com.koutilya.monolith.outbox.OutboxEvent;
import com.koutilya.monolith.outbox.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Orders application service in the monolith. Every state change to an order and the outbox
 * event describing that change are written in a SINGLE transaction (see {@link #createOrder}
 * and {@link #transition}). This is the heart of the strangler migration: it guarantees that
 * the change and the "something changed" signal commit atomically, so there is no window in
 * which the DB and the event stream disagree.
 */
@Service
public class OrderService {

    static final String AGGREGATE_TYPE = "Order";

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        OutboxEventRepository outboxRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Create an order and enqueue its outbox event atomically. If either write fails the whole
     * transaction rolls back, leaving neither the order nor a phantom event behind.
     */
    @Transactional
    public OrderEntity createOrder(Long customerId, BigDecimal totalAmount) {
        OrderEntity order = new OrderEntity(UUID.randomUUID().toString(), customerId, totalAmount);
        orderRepository.save(order);
        appendOutboxEvent(order, "OrderCreated");
        return order;
    }

    @Transactional
    public OrderEntity transition(String orderId, OrderStatus newStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("order not found: " + orderId));
        order.transitionTo(newStatus);
        // save() is technically redundant under a managed entity, but kept explicit for clarity.
        orderRepository.save(order);
        appendOutboxEvent(order, "OrderStatusChanged");
        return order;
    }

    /**
     * Idempotently apply a mirrored write coming back from the extracted service during the canary
     * WRITE phase. Symmetric to the consumer's replication: only a strictly-newer version is
     * applied, so the mirror is safe to retry and cannot regress the monolith's state.
     * <p>
     * NOTE: a mirror deliberately does NOT append an outbox event — otherwise the change would loop
     * back to the service via CDC. The version guard makes such a loop a no-op anyway, but skipping
     * the outbox write keeps the stream clean.
     */
    @Transactional
    public OrderEntity mirrorUpsert(String id, Long customerId, OrderStatus status,
                                    BigDecimal totalAmount, long aggregateVersion) {
        OrderEntity existing = orderRepository.findById(id).orElse(null);
        if (existing == null) {
            return orderRepository.save(new OrderEntity(id, customerId, totalAmount, status, aggregateVersion));
        }
        if (existing.getAggregateVersion() < aggregateVersion) {
            existing.applyMirror(status, totalAmount, aggregateVersion);
            orderRepository.save(existing);
        }
        return existing;
    }

    @Transactional(readOnly = true)
    public OrderEntity getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> listByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    private void appendOutboxEvent(OrderEntity order, String eventType) {
        OrderEventPayload payload = new OrderEventPayload(
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getAggregateVersion());
        outboxRepository.save(new OutboxEvent(
                AGGREGATE_TYPE,
                order.getId(),
                eventType,
                serialize(payload)));
    }

    private String serialize(OrderEventPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // Serialization of a small record cannot realistically fail; surfacing it aborts the tx.
            throw new IllegalStateException("failed to serialize order event payload", e);
        }
    }
}
