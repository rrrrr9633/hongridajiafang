package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "product_types")
public class ProductType {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 255)
    private String intro;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "image_asset_id")
    private UUID imageAssetId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean active;

    protected ProductType() {
    }

    public ProductType(String id, String productId, String name, String intro, String imageUrl, int sortOrder, boolean active) {
        this.id = id;
        this.productId = productId;
        update(name, intro, imageUrl, sortOrder, active);
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getIntro() {
        return intro;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public UUID getImageAssetId() {
        return imageAssetId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void update(String name, String intro, String imageUrl, int sortOrder, boolean active) {
        this.name = name == null || name.isBlank() ? "默认款" : name.trim();
        this.intro = intro == null || intro.isBlank() ? "" : intro.trim();
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public void setImageAssetId(UUID imageAssetId) {
        this.imageAssetId = imageAssetId;
    }
}
