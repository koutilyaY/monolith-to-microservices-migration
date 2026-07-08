package com.koutilya.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Legacy commerce monolith. Owns the Customers and Orders bounded contexts in a single
 * Postgres schema. This is the system being strangled: the {@code orders-service} will
 * gradually take over the Orders context while Customers stays here.
 */
@SpringBootApplication
@EnableScheduling
public class MonolithApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonolithApplication.class, args);
    }
}
