package com.koutilya.gateway.canary;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Weighted, runtime-controllable routing for the strangle.
 * <p>
 * Two routes match {@code /api/orders/**}: the new-service route carries an extra predicate that
 * fires only for the canary share of traffic ({@link CanaryState#routeToNewService}); when it does
 * not fire, the request falls through to the monolith route. Because predicates are evaluated on
 * every request, weight changes (and rollback to 0) take effect immediately. Customers is not
 * extracted, so {@code /api/customers/**} always goes to the monolith.
 */
@Configuration
public class CanaryRoutingConfig {

    @Bean
    public RouteLocator strangleRoutes(RouteLocatorBuilder builder,
                                       CanaryState canaryState,
                                       @Value("${migration.routing.monolith-uri:http://localhost:8081}") String monolithUri,
                                       @Value("${migration.routing.orders-service-uri:http://localhost:8082}") String ordersServiceUri) {
        return builder.routes()
                // Canary: the share of Orders traffic routed to the extracted service.
                .route("orders-new-service", r -> r
                        .path("/api/orders/**")
                        .and()
                        .predicate(canaryState::routeToNewService)
                        .filters(f -> f.addResponseHeader("X-Order-Backend", "orders-service"))
                        .uri(ordersServiceUri))
                // Fallback for the remaining Orders traffic (and 100% after a rollback).
                .route("orders-monolith", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f.addResponseHeader("X-Order-Backend", "monolith"))
                        .uri(monolithUri))
                // Customers is still owned by the monolith (partial strangle).
                .route("customers-monolith", r -> r
                        .path("/api/customers/**")
                        .filters(f -> f.addResponseHeader("X-Order-Backend", "monolith"))
                        .uri(monolithUri))
                .build();
    }
}
