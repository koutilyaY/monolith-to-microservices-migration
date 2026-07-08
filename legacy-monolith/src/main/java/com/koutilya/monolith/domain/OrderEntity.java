package com.koutilya.monolith.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The Orders aggregate root. Its id is a client-independent UUID string so that the same
 * identity survives the move into {@code orders-service} (no id remapping at cutover).
 * <p>
 * {@code aggregateVersion} is a monotonically increasing per-order version. It is the basis
 * for idempotent, out-of-order-safe replication into the new service: a consumer only applies
 * an event whose version is strictly greater than the version it has already stored.
 */
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "aggregate_version", nullable = false)
    private long aggregateVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderEntity() {
    }

    public OrderEntity(String id, Long customerId, BigDecimal totalAmount) {
        this(id, customerId, totalAmount, OrderStatus.PLACED, 1L);
    }

    /** Full constructor used when mirroring an order created by the extracted service. */
    public OrderEntity(String id, Long customerId, BigDecimal totalAmount, OrderStatus status, long aggregateVersion) {
        this.id = id;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.aggregateVersion = aggregateVersion;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Idempotently adopt state from a mirrored write; callers guarantee a strictly newer version. */
    public void applyMirror(OrderStatus status, BigDecimal totalAmount, long aggregateVersion) {
        this.status = status;
        this.totalAmount = totalAmount;
        this.aggregateVersion = aggregateVersion;
        this.updatedAt = Instant.now();
    }

    /**
     * Apply a status transition and bump the aggregate version. Callers must persist an outbox
     * event carrying the new version in the same transaction.
     */
    public void transitionTo(OrderStatus newStatus) {
        this.status = newStatus;
        this.aggregateVersion += 1;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public long getAggregateVersion() {
        return aggregateVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
