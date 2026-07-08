package com.koutilya.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The extracted Orders microservice. It owns its own datastore, stays consistent with the
 * monolith during the dual-run by consuming CDC events, and serves the same {@code /api/orders}
 * contract so the gateway can route traffic to it transparently.
 */
@SpringBootApplication
public class OrdersServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrdersServiceApplication.class, args);
    }
}
