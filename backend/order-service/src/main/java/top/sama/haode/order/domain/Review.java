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
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    @Column(nullable = false)
    private Integer rating;
    @Column(nullable = false, length = 1000)
    private String content;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Review() {}

    public Review(UUID orderId, String userId, int rating, String content) {
        this.orderId = orderId;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
        this.status = ReviewStatus.PENDING_REVIEW;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public Integer getRating() { return rating; }
    public String getContent() { return content; }
    public ReviewStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void approve() { status = ReviewStatus.APPROVED; updatedAt = Instant.now(); }
    public void reject() { status = ReviewStatus.REJECTED; updatedAt = Instant.now(); }
}
