package com.koutilya.orders.service;

import com.koutilya.orders.domain.Order;

/**
 * Dual-write channel back to the monolith during the canary WRITE phase. While the new service is
 * authoritative for the writes routed to it, it mirrors them into the monolith so the not-yet-
 * extracted parts of the system (e.g. Customers reporting that still joins orders) keep seeing a
 * complete picture, and so an instant rollback to the monolith loses nothing.
 */
public interface MonolithMirrorClient {

    /**
     * Best-effort mirror of a locally-created order into the monolith.
     *
     * @return true if the monolith accepted the write, false if it was skipped or failed (the
     * caller then relies on reconciliation; see README "dual-write partial failure").
     */
    boolean mirror(Order order);
}
