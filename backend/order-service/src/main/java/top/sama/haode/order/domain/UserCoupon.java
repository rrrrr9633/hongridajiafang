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
@Table(name = "user_coupons")
public class UserCoupon {
    public enum Status { UNUSED, LOCKED, USED }
    public enum Source { INVITE, INVITE_PURCHASE, ADMIN }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "threshold_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Source source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "checkout_id")
    private UUID checkoutId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected UserCoupon() {
    }

    public UserCoupon(String userId, CouponTemplate template, Source source) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.templateId = template.getId();
        this.name = template.getName();
        this.thresholdAmount = template.getThresholdAmount();
        this.discountAmount = template.getDiscountAmount();
        this.source = source;
        this.status = Status.UNUSED;
        this.createdAt = Instant.now();
    }

    public boolean usableFor(BigDecimal goodsAmount) {
        return status == Status.UNUSED
                && goodsAmount != null
                && goodsAmount.compareTo(thresholdAmount) >= 0
                && goodsAmount.subtract(discountAmount).signum() > 0;
    }

    public void lock(UUID checkoutId) {
        if (status != Status.UNUSED) throw new IllegalStateException("优惠券不可用");
        this.status = Status.LOCKED;
        this.checkoutId = checkoutId;
    }

    public void release() {
        if (status != Status.LOCKED) return;
        this.status = Status.UNUSED;
        this.checkoutId = null;
    }

    public void markUsed() {
        if (status == Status.USED) return;
        if (status != Status.LOCKED) throw new IllegalStateException("优惠券未锁定在结算单上");
        this.status = Status.USED;
        this.usedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public UUID getTemplateId() { return templateId; }
    public String getName() { return name; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public Source getSource() { return source; }
    public Status getStatus() { return status; }
    public UUID getCheckoutId() { return checkoutId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUsedAt() { return usedAt; }
}
