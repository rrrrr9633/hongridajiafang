package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "after_sales")
public class AfterSale {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AfterSaleStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AfterSale() {
    }

    public AfterSale(UUID orderId, String userId, String reason) {
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
        this.status = AfterSaleStatus.REQUESTED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getReason() { return reason; }
    public AfterSaleStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
