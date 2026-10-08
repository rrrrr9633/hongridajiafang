package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "coupon_policies")
public class CouponPolicy {
    public static final String DEFAULT_ID = "default";

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "invite_template_id")
    private UUID inviteTemplateId;

    @Column(name = "purchase_template_id")
    private UUID purchaseTemplateId;

    @Column(name = "purchase_threshold", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchaseThreshold;

    protected CouponPolicy() {
    }

    public CouponPolicy(String id) {
        this.id = id;
        this.enabled = false;
        this.purchaseThreshold = BigDecimal.ZERO;
    }

    public void replace(boolean enabled, UUID inviteTemplateId, UUID purchaseTemplateId, BigDecimal purchaseThreshold) {
        if (purchaseThreshold == null || purchaseThreshold.signum() < 0) {
            throw new IllegalArgumentException("消费达标金额不能为负");
        }
        this.enabled = enabled;
        this.inviteTemplateId = inviteTemplateId;
        this.purchaseTemplateId = purchaseTemplateId;
        this.purchaseThreshold = purchaseThreshold.setScale(2, RoundingMode.HALF_UP);
    }

    public String getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public UUID getInviteTemplateId() { return inviteTemplateId; }
    public UUID getPurchaseTemplateId() { return purchaseTemplateId; }
    public BigDecimal getPurchaseThreshold() { return purchaseThreshold; }
}
