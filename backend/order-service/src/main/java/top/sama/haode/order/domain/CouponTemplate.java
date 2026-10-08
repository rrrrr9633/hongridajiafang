package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupon_templates")
public class CouponTemplate {
    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "threshold_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "per_order_limit", nullable = false)
    private int perOrderLimit;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CouponTemplate() {
    }

    public CouponTemplate(String name, BigDecimal thresholdAmount, BigDecimal discountAmount, boolean active, int perOrderLimit) {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        replace(name, thresholdAmount, discountAmount, active, perOrderLimit);
    }

    public void replace(String name, BigDecimal thresholdAmount, BigDecimal discountAmount, boolean active, int perOrderLimit) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("请填写优惠券名称");
        BigDecimal threshold = requireMoney(thresholdAmount, "满减门槛");
        BigDecimal discount = requireMoney(discountAmount, "减免金额");
        if (threshold.signum() < 0) throw new IllegalArgumentException("满减门槛不能为负");
        if (discount.signum() <= 0) throw new IllegalArgumentException("减免金额必须大于 0");
        if (perOrderLimit < 1 || perOrderLimit > 999) throw new IllegalArgumentException("每单上限需在 1 到 999 之间");
        this.name = name.trim();
        this.thresholdAmount = threshold;
        this.discountAmount = discount;
        this.active = active;
        this.perOrderLimit = perOrderLimit;
        this.updatedAt = Instant.now();
    }

    private static BigDecimal requireMoney(BigDecimal value, String label) {
        if (value == null) throw new IllegalArgumentException(label + "无效");
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public boolean isActive() { return active; }
    public int getPerOrderLimit() { return perOrderLimit; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
