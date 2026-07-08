package com.koutilya.orders.service;

import com.koutilya.orders.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * HTTP mirror to the monolith's original Orders API. Active only when dual-write is enabled
 * (canary write phase). Failures are swallowed to a boolean + log: a failed mirror must NOT fail
 * the customer's request, because the new service already committed the write it is authoritative
 * for. Divergence is closed by the periodic reconciliation job (documented, not fully built here).
 */
@Component
@ConditionalOnProperty(value = "migration.dual-write.enabled", havingValue = "true")
public class HttpMonolithMirrorClient implements MonolithMirrorClient {

    private static final Logger log = LoggerFactory.getLogger(HttpMonolithMirrorClient.class);

    private final RestClient restClient;

    public HttpMonolithMirrorClient(RestClient.Builder builder,
                                    @Value("${migration.monolith.base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public boolean mirror(Order order) {
        try {
            restClient.post()
                    .uri("/internal/orders/mirror")
                    .body(Map.of(
                            "id", order.getId(),
                            "customerId", order.getCustomerId(),
                            "status", order.getStatus(),
                            "totalAmount", order.getTotalAmount(),
                            "version", order.getVersion()))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RuntimeException e) {
            log.warn("Dual-write mirror to monolith failed for order {} (v{}); leaving to reconciliation: {}",
                    order.getId(), order.getVersion(), e.getMessage());
            return false;
        }
    }
}
