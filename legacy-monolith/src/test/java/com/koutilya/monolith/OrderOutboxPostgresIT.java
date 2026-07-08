package com.koutilya.monolith;

import com.koutilya.monolith.domain.OrderEntity;
import com.koutilya.monolith.outbox.OutboxEventRepository;
import com.koutilya.monolith.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test against a REAL Postgres (Testcontainers). Excluded from the default build;
 * runs only under {@code mvn -Pit verify}. Verifies the outbox invariant holds on the actual
 * target database engine, not just H2.
 */
@Testcontainers
@SpringBootTest
class OrderOutboxPostgresIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    OrderService orderService;
    @Autowired
    OutboxEventRepository outboxRepository;

    @Test
    void outboxRowIsWrittenInSameTransaction() {
        OrderEntity order = orderService.createOrder(1L, new BigDecimal("55.00"));
        assertThat(outboxRepository.findAll())
                .anyMatch(e -> e.getAggregateId().equals(order.getId()));
    }
}
