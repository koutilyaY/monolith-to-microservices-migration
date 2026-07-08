package com.koutilya.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The strangler facade. All client traffic enters here. It weight-routes the Orders bounded
 * context between the legacy monolith and the extracted orders-service (the canary), while
 * Customers always goes to the monolith (partial strangle). An SLO watcher can instantly flip
 * all Orders traffic back to the monolith on a breach.
 */
@SpringBootApplication
@EnableScheduling
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
