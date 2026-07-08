package com.koutilya.orders.service;

import com.koutilya.orders.domain.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fallback when dual-write is off (e.g. after full cutover, when the monolith no longer owns
 * orders). Keeps {@link OrderCommandService} unconditional so there is no null-check at the seam.
 */
@Component
@ConditionalOnProperty(value = "migration.dual-write.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMonolithMirrorClient implements MonolithMirrorClient {
    @Override
    public boolean mirror(Order order) {
        return false;
    }
}
