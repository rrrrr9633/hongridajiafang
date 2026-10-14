package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupon_usage_rules")
public class CouponUsageRule {
    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false)
    private int priority;

    @Lob
    @Column(name = "zones_json", nullable = false)
    private String zonesJson;

    @Lob
    @Column(name = "product_ids_json", nullable = false)
    private String productIdsJson;

    @Lob
    @Column(name = "options_json", nullable = false)
    private String optionsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CouponUsageRule() {
    }

    public CouponUsageRule(String name, int priority, String zonesJson, String productIdsJson, String optionsJson) {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        replace(name, priority, zonesJson, productIdsJson, optionsJson);
    }

    public void replace(String name, int priority, String zonesJson, String productIdsJson, String optionsJson) {
        this.name = name;
        this.priority = priority;
        this.zonesJson = zonesJson;
        this.productIdsJson = productIdsJson;
        this.optionsJson = optionsJson;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getPriority() { return priority; }
    public String getZonesJson() { return zonesJson; }
    public String getProductIdsJson() { return productIdsJson; }
    public String getOptionsJson() { return optionsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
