package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 255)
    private String subtitle;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(length = 32)
    private String color;

    @Column(length = 32)
    private String accent;

    @Column(length = 64)
    private String pattern;

    @Column(length = 64)
    private String tag;

    @Column(nullable = false, length = 64)
    private String zone;

    @Column(nullable = false)
    private Boolean homepage;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "image_asset_id")
    private UUID imageAssetId;

    @Column(nullable = false)
    private Boolean active;

    protected Product() {
    }

    public Product(String id, String name, String subtitle, BigDecimal price, BigDecimal originalPrice,
                   String color, String accent, String pattern, String tag, String zone, boolean homepage,
                   int sortOrder, String imageUrl, boolean active) {
        this.id = id;
        update(name, subtitle, price, originalPrice, color, accent, pattern, tag, zone, homepage, sortOrder, imageUrl, active);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public String getColor() {
        return color;
    }

    public String getAccent() {
        return accent;
    }

    public String getPattern() {
        return pattern;
    }

    public String getTag() {
        return tag;
    }

    public String getZone() {
        return zone;
    }

    public Boolean getHomepage() {
        return homepage;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public UUID getImageAssetId() {
        return imageAssetId;
    }

    public void update(String name, String subtitle, BigDecimal price, BigDecimal originalPrice,
                       String color, String accent, String pattern, String tag, String zone, boolean homepage,
                       int sortOrder, String imageUrl, boolean active) {
        this.name = name;
        this.subtitle = subtitle;
        this.price = price;
        this.originalPrice = originalPrice;
        this.color = color;
        this.accent = accent;
        this.pattern = pattern;
        this.tag = tag;
        this.zone = (zone == null || zone.isBlank()) ? "床品区" : zone.trim();
        this.homepage = homepage;
        this.sortOrder = sortOrder;
        this.imageUrl = imageUrl;
        this.active = active;
    }

    public void setImageAssetId(UUID imageAssetId) {
        this.imageAssetId = imageAssetId;
    }

    public void deactivate() {
        this.active = false;
    }
}
