package com.koutilya.orders.service;

import com.koutilya.orders.domain.Order;
import com.koutilya.orders.domain.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Order getOrder(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> listByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
}
