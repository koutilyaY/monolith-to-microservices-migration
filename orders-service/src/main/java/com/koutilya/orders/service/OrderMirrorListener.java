package com.koutilya.orders.service;

import com.koutilya.orders.domain.OrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fires the dual-write mirror strictly AFTER the local order transaction commits. If the mirror
 * fails, the local order still stands (the new service is authoritative) and reconciliation closes
 * the gap. This is the deliberate answer to the classic dual-write partial-failure problem: we
 * never attempt two writes inside one distributed transaction; we commit one and best-effort the
 * other, keeping the authoritative side always correct.
 */
@Component
public class OrderMirrorListener {

    private final OrderRepository orderRepository;
    private final MonolithMirrorClient mirrorClient;

    public OrderMirrorListener(OrderRepository orderRepository, MonolithMirrorClient mirrorClient) {
        this.orderRepository = orderRepository;
        this.mirrorClient = mirrorClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedLocally event) {
        orderRepository.findById(event.orderId()).ifPresent(mirrorClient::mirror);
    }
}
