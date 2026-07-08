package com.koutilya.orders.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koutilya.orders.events.OrderEvent;
import com.koutilya.orders.service.OrderReplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order change events shipped by Debezium from the monolith outbox. The Debezium outbox
 * event router unwraps the {@code payload} column, so the Kafka message value is exactly the
 * {@link OrderEvent} JSON. Delivery is at-least-once; idempotency + ordering are handled downstream
 * by {@link OrderReplicationService#apply} using the aggregate version, so this method can safely
 * be re-invoked with duplicates.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderReplicationService replicationService;

    public OrderEventConsumer(ObjectMapper objectMapper, OrderReplicationService replicationService) {
        this.objectMapper = objectMapper;
        this.replicationService = replicationService;
    }

    @KafkaListener(topics = "${migration.orders-events-topic:orders.events}",
            groupId = "${spring.kafka.consumer.group-id:orders-service}")
    public void onMessage(String message) {
        OrderEvent event = deserialize(message);
        if (event == null) {
            return;
        }
        boolean applied = replicationService.apply(event);
        log.debug("Consumed order {} v{} applied={}", event.orderId(), event.aggregateVersion(), applied);
    }

    private OrderEvent deserialize(String message) {
        try {
            return objectMapper.readValue(message, OrderEvent.class);
        } catch (Exception e) {
            // Poison message: log and move on rather than blocking the partition. A real deployment
            // would route this to a dead-letter topic via the container error handler.
            log.error("Failed to deserialize order event; dropping. payload={}", message, e);
            return null;
        }
    }
}
