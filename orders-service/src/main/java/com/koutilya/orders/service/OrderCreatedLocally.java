package com.koutilya.orders.service;

/** Internal domain event: an order was created locally and (if dual-write is on) needs mirroring. */
public record OrderCreatedLocally(String orderId) {
}
