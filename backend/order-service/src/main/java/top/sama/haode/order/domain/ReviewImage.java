package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "review_images")
public class ReviewImage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;
    @Column(name = "asset_id", nullable = false)
    private UUID assetId;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    protected ReviewImage() {}

    public ReviewImage(UUID reviewId, UUID assetId, int sortOrder) {
        this.reviewId = reviewId;
        this.assetId = assetId;
        this.sortOrder = sortOrder;
    }

    public UUID getReviewId() { return reviewId; }
    public UUID getAssetId() { return assetId; }
    public Integer getSortOrder() { return sortOrder; }
}
