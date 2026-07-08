package com.koutilya.orders.service;

import com.koutilya.orders.domain.Order;
import com.koutilya.orders.domain.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderMirrorListenerTest {

    private final OrderRepository repository = mock(OrderRepository.class);
    private final MonolithMirrorClient mirror = mock(MonolithMirrorClient.class);
    private final OrderMirrorListener listener = new OrderMirrorListener(repository, mirror);

    @Test
    void mirrorsExistingOrderAfterCommit() {
        Order order = new Order("o-1", 42L, new BigDecimal("10.00"), "PLACED", 1L);
        when(repository.findById("o-1")).thenReturn(Optional.of(order));

        listener.onOrderCreated(new OrderCreatedLocally("o-1"));

        verify(mirror).mirror(order);
    }

    @Test
    void skipsMirrorWhenOrderVanished() {
        when(repository.findById("gone")).thenReturn(Optional.empty());

        listener.onOrderCreated(new OrderCreatedLocally("gone"));

        verify(mirror, never()).mirror(org.mockito.ArgumentMatchers.any());
    }
}
