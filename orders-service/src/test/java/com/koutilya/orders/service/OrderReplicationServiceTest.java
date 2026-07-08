package com.koutilya.orders.service;

import com.koutilya.orders.domain.Order;
import com.koutilya.orders.domain.OrderRepository;
import com.koutilya.orders.events.OrderEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the consumer's idempotency + ordering guarantee against a real (H2) transaction.
 * This is the safety net that makes at-least-once CDC delivery correct.
 */
@DataJpaTest
@Import(OrderReplicationService.class)
class OrderReplicationServiceTest {

    private static final String ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    @Autowired
    OrderReplicationService replication;
    @Autowired
    OrderRepository repository;

    private OrderEvent event(String status, long version) {
        return new OrderEvent(ID, 42L, status, new BigDecimal("50.00"), version);
    }

    @Test
    void firstEventCreatesOrder() {
        assertThat(replication.apply(event("PLACED", 1))).isTrue();

        Order stored = repository.findById(ID).orElseThrow();
        assertThat(stored.getVersion()).isEqualTo(1);
        assertThat(stored.getStatus()).isEqualTo("PLACED");
    }

    @Test
    void redeliveryOfSameVersionIsNoOp() {
        replication.apply(event("PLACED", 1));

        assertThat(replication.apply(event("PLACED", 1))).isFalse();
        assertThat(repository.findById(ID).orElseThrow().getVersion()).isEqualTo(1);
    }

    @Test
    void newerVersionIsApplied() {
        replication.apply(event("PLACED", 1));

        assertThat(replication.apply(event("PAID", 2))).isTrue();
        Order stored = repository.findById(ID).orElseThrow();
        assertThat(stored.getVersion()).isEqualTo(2);
        assertThat(stored.getStatus()).isEqualTo("PAID");
    }

    @Test
    void staleOutOfOrderEventIsIgnored() {
        replication.apply(event("PLACED", 1));
        replication.apply(event("PAID", 2));

        // A late-arriving v1 must NOT clobber the newer v2 state.
        assertThat(replication.apply(event("PLACED", 1))).isFalse();
        Order stored = repository.findById(ID).orElseThrow();
        assertThat(stored.getVersion()).isEqualTo(2);
        assertThat(stored.getStatus()).isEqualTo("PAID");
    }
}
