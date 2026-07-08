package com.koutilya.orders.service;

import com.koutilya.orders.domain.Order;
import com.koutilya.orders.domain.OrderRepository;
import com.koutilya.orders.events.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies replicated order events from the CDC stream into the new service's own store.
 * <p>
 * Idempotency + ordering are enforced by the aggregate {@code version}: an event is applied only
 * if its version is STRICTLY GREATER than what we already have. This single rule makes the
 * consumer safe against the three things at-least-once CDC throws at you:
 * <ul>
 *   <li>Redelivery of the same event  -> version equal        -> no-op</li>
 *   <li>Out-of-order / stale delivery -> version lower         -> no-op</li>
 *   <li>A genuinely newer change      -> version higher        -> applied</li>
 * </ul>
 * Because the check and the write happen in one transaction, concurrent consumers converge to the
 * highest-versioned state without locks beyond the row itself.
 */
@Service
public class OrderReplicationService {

    private static final Logger log = LoggerFactory.getLogger(OrderReplicationService.class);

    private final OrderRepository orderRepository;

    public OrderReplicationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * @return true if the event advanced local state, false if it was a duplicate / stale no-op.
     */
    @Transactional
    public boolean apply(OrderEvent event) {
        Order existing = orderRepository.findById(event.orderId()).orElse(null);

        if (existing != null && existing.getVersion() >= event.aggregateVersion()) {
            log.debug("Skipping order {} event v{} (already at v{})",
                    event.orderId(), event.aggregateVersion(), existing.getVersion());
            return false;
        }

        if (existing == null) {
            orderRepository.save(new Order(
                    event.orderId(),
                    event.customerId(),
                    event.totalAmount(),
                    event.status(),
                    event.aggregateVersion()));
        } else {
            existing.applyFrom(event);
            orderRepository.save(existing);
        }
        log.debug("Applied order {} at v{}", event.orderId(), event.aggregateVersion());
        return true;
    }
}
