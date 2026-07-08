package com.koutilya.monolith.backfill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koutilya.monolith.domain.OrderEntity;
import com.koutilya.monolith.domain.OrderRepository;
import com.koutilya.monolith.outbox.OrderEventPayload;
import com.koutilya.monolith.outbox.OutboxEvent;
import com.koutilya.monolith.outbox.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-off historical backfill, enabled only when the app is started with {@code --backfill}.
 * <p>
 * It re-emits an outbox event for every existing order so that the SAME CDC pipeline that ships
 * live changes also ships the historical rows. This deliberately reuses the live path instead of
 * a side channel: because the consumer upserts idempotently by (orderId, version), the backfill
 * and live traffic can run concurrently and safely converge — a backfilled event carrying an old
 * version can never clobber a newer live update. See README "Backfill vs live traffic race".
 */
@Component
public class OrdersBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrdersBackfillRunner.class);

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrdersBackfillRunner(OrderRepository orderRepository,
                                OutboxEventRepository outboxRepository,
                                ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("backfill")) {
            return;
        }
        long count = backfill();
        log.info("Backfill complete: re-emitted outbox events for {} historical orders", count);
    }

    @Transactional
    public long backfill() {
        long count = 0;
        for (OrderEntity order : orderRepository.findAll()) {
            OrderEventPayload payload = new OrderEventPayload(
                    order.getId(),
                    order.getCustomerId(),
                    order.getStatus().name(),
                    order.getTotalAmount(),
                    order.getAggregateVersion());
            try {
                outboxRepository.save(new OutboxEvent(
                        "Order", order.getId(), "OrderBackfill", objectMapper.writeValueAsString(payload)));
                count++;
            } catch (Exception e) {
                throw new IllegalStateException("backfill failed for order " + order.getId(), e);
            }
        }
        return count;
    }
}
