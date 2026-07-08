package com.koutilya.orders.domain;

import com.koutilya.orders.events.OrderEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Orders aggregate as owned by the new service. Same id space as the monolith (UUID string) so
 * replicated and natively-created orders share one identity domain. {@code version} mirrors the
 * monolith's aggregate version and is the idempotency / ordering key for replication.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(String id, Long customerId, BigDecimal totalAmount, String status, long version) {
        this.id = id;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.version = version;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Overwrite mutable state from a newer replicated event. Caller guarantees monotonic version. */
    public void applyFrom(OrderEvent event) {
        this.customerId = event.customerId();
        this.status = event.status();
        this.totalAmount = event.totalAmount();
        this.version = event.aggregateVersion();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
