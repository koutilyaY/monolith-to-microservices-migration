package com.koutilya.monolith.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koutilya.monolith.domain.OrderEntity;
import com.koutilya.monolith.domain.OrderRepository;
import com.koutilya.monolith.domain.OrderStatus;
import com.koutilya.monolith.outbox.OutboxEvent;
import com.koutilya.monolith.outbox.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the transactional-outbox invariant: a business write and its outbox event are produced
 * together against a real (H2) transaction. Runs offline in the default build.
 */
@DataJpaTest
@Import({OrderService.class, OrderServiceOutboxTest.Config.class})
class OrderServiceOutboxTest {

    @TestConfiguration
    static class Config {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    OutboxEventRepository outboxRepository;

    @Test
    void createOrder_writesOrderAndOutboxEventAtomically() {
        OrderEntity order = orderService.createOrder(42L, new BigDecimal("199.99"));

        assertThat(orderRepository.findById(order.getId())).isPresent();

        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);
        OutboxEvent event = events.get(0);
        assertThat(event.getAggregateType()).isEqualTo("Order");
        assertThat(event.getAggregateId()).isEqualTo(order.getId());
        assertThat(event.getType()).isEqualTo("OrderCreated");
        assertThat(event.getPayload())
                .contains("\"aggregateVersion\":1")
                .contains("\"customerId\":42")
                .contains(order.getId());
    }

    @Test
    void mirrorUpsert_isIdempotentByVersionAndWritesNoOutbox() {
        String id = "33333333-3333-3333-3333-333333333333";

        orderService.mirrorUpsert(id, 9L, OrderStatus.PLACED, new BigDecimal("20.00"), 1L);
        // stale re-mirror must not regress
        orderService.mirrorUpsert(id, 9L, OrderStatus.PLACED, new BigDecimal("20.00"), 1L);
        // newer version applied
        orderService.mirrorUpsert(id, 9L, OrderStatus.PAID, new BigDecimal("20.00"), 2L);

        assertThat(orderRepository.findById(id)).get()
                .satisfies(o -> {
                    assertThat(o.getAggregateVersion()).isEqualTo(2L);
                    assertThat(o.getStatus()).isEqualTo(OrderStatus.PAID);
                });
        // Mirrors must NOT append outbox events (avoids a CDC feedback loop).
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void transition_bumpsVersionAndEmitsSecondEvent() {
        OrderEntity order = orderService.createOrder(7L, new BigDecimal("10.00"));

        OrderEntity updated = orderService.transition(order.getId(), OrderStatus.PAID);

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(updated.getAggregateVersion()).isEqualTo(2L);
        assertThat(outboxRepository.findAll()).hasSize(2);
        assertThat(outboxRepository.findAll())
                .anyMatch(e -> e.getType().equals("OrderStatusChanged")
                        && e.getPayload().contains("\"aggregateVersion\":2")
                        && e.getPayload().contains("\"status\":\"PAID\""));
    }
}
