package com.koutilya.orders;

import com.koutilya.orders.domain.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end replication test against REAL Kafka + Postgres (Testcontainers). Publishes an outbox
 * payload to the orders topic and asserts the consumer idempotently materializes it. Runs only
 * under {@code mvn -Pit verify}.
 */
@Testcontainers
@SpringBootTest
class OrderReplicationKafkaIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
    }

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    OrderRepository orderRepository;

    @Test
    void publishedOutboxEventIsMaterializedIdempotently() throws InterruptedException {
        String payload = "{\"orderId\":\"it-1\",\"customerId\":42,\"status\":\"PLACED\","
                + "\"totalAmount\":199.99,\"aggregateVersion\":1}";

        kafkaTemplate.send("orders.events", "it-1", payload);
        // duplicate + stale re-delivery must not corrupt state
        kafkaTemplate.send("orders.events", "it-1", payload);

        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(deadline) && orderRepository.findById("it-1").isEmpty()) {
            Thread.sleep(250);
        }

        assertThat(orderRepository.findById("it-1")).isPresent();
        assertThat(orderRepository.findById("it-1").orElseThrow().getVersion()).isEqualTo(1);
    }
}
