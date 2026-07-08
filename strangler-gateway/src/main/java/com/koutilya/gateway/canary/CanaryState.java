package com.koutilya.gateway.canary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runtime-mutable canary weight for the Orders context. {@code newServiceWeight} is the percentage
 * (0-100) of {@code /api/orders/**} traffic sent to the extracted orders-service; the remainder
 * goes to the monolith. Changing the weight takes effect on the very next request (predicates are
 * evaluated per-request), which is what makes rollback instant: set the weight to 0.
 */
@Component
public class CanaryState {

    private static final Logger log = LoggerFactory.getLogger(CanaryState.class);

    private final AtomicInteger newServiceWeight = new AtomicInteger(0);
    private volatile boolean rolledBack = false;
    private volatile String lastChangeReason = "initial";

    public CanaryState(@Value("${migration.canary.initial-weight:0}") int initialWeight) {
        this.newServiceWeight.set(clamp(initialWeight));
    }

    /** Per-request routing decision used as the gateway predicate for the new-service route. */
    public boolean routeToNewService(ServerWebExchange exchange) {
        int weight = newServiceWeight.get();
        if (weight >= 100) {
            return true;
        }
        if (weight <= 0) {
            return false;
        }
        return ThreadLocalRandom.current().nextInt(100) < weight;
    }

    public int getNewServiceWeight() {
        return newServiceWeight.get();
    }

    /** Operator action: advance (or lower) the canary weight. Clears the rolled-back flag. */
    public void setNewServiceWeight(int weight, String reason) {
        int clamped = clamp(weight);
        this.newServiceWeight.set(clamped);
        this.rolledBack = false;
        this.lastChangeReason = reason;
        log.info("Canary weight set to {}% for orders-service (reason: {})", clamped, reason);
    }

    /** Automated (or manual) instant rollback: all Orders traffic returns to the monolith. */
    public void rollback(String reason) {
        this.newServiceWeight.set(0);
        this.rolledBack = true;
        this.lastChangeReason = reason;
        log.warn("CANARY ROLLBACK: orders-service weight forced to 0 (reason: {})", reason);
    }

    public boolean isRolledBack() {
        return rolledBack;
    }

    public String getLastChangeReason() {
        return lastChangeReason;
    }

    private static int clamp(int weight) {
        return Math.max(0, Math.min(100, weight));
    }
}
