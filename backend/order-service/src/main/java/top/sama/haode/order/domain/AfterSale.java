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

    @Column(name = "previous_status", length = 32)
    private String previousStatus;

    @Column(name = "out_refund_no", length = 64)
    private String outRefundNo;

    @Column(name = "wechat_refund_id", length = 64)
    private String wechatRefundId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AfterSale() {
    }

    public AfterSale(UUID orderId, String userId, String reason, OrderStatus previousStatus) {
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
        this.status = AfterSaleStatus.REQUESTED;
        this.previousStatus = previousStatus == null ? null : previousStatus.name();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void approve(String outRefundNo) {
        if (status == AfterSaleStatus.APPROVED || status == AfterSaleStatus.REFUNDED) return;
        if (status != AfterSaleStatus.REQUESTED) throw new IllegalStateException("售后单当前状态不可同意");
        this.status = AfterSaleStatus.APPROVED;
        this.outRefundNo = outRefundNo;
        this.updatedAt = Instant.now();
    }

    public void reject() {
        if (status == AfterSaleStatus.REJECTED) return;
        if (status != AfterSaleStatus.REQUESTED) throw new IllegalStateException("售后单当前状态不可拒绝");
        this.status = AfterSaleStatus.REJECTED;
        this.updatedAt = Instant.now();
    }

    public void markRefunded(String wechatRefundId) {
        this.status = AfterSaleStatus.REFUNDED;
        if (wechatRefundId != null && !wechatRefundId.isBlank()) this.wechatRefundId = wechatRefundId;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getReason() { return reason; }
    public AfterSaleStatus getStatus() { return status; }
    public String getPreviousStatus() { return previousStatus; }
    public String getOutRefundNo() { return outRefundNo; }
    public String getWechatRefundId() { return wechatRefundId; }
    public Instant getCreatedAt() { return createdAt; }
}
