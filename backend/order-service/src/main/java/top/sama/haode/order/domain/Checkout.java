package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkouts")
public class Checkout {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Checkout() {}

    public Checkout(String userId, BigDecimal amount) {
        this(UUID.randomUUID(), userId, amount);
    }

    public Checkout(UUID id, String userId, BigDecimal amount) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("结算金额无效");
        this.amount = amount;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) return;
        if (status != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("结算单当前状态不可取消");
        status = OrderStatus.CANCELLED;
        updatedAt = Instant.now();
    }

    public void markPaid() {
        if (status != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("结算单当前状态不可支付");
        status = OrderStatus.PAID;
        updatedAt = Instant.now();
    }
}
