package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "checkout_id")
    private UUID checkoutId;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    @Column(name = "product_type_id", length = 64)
    private String productTypeId;

    @Column(name = "product_name", nullable = false, length = 128)
    private String productName;

    @Column(name = "product_type_name", length = 64)
    private String productTypeName;

    @Column(name = "product_subtitle", nullable = false, length = 255)
    private String productSubtitle;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "tracking_no", length = 64)
    private String trackingNo;

    @Column(name = "express_company", length = 32)
    private String expressCompany;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "shipping_uploaded_at")
    private Instant shippingUploadedAt;

    protected Order() {
    }

    public Order(String userId, Product product, ProductType type, int quantity) {
        this(userId, null, product, type, quantity);
    }

    public Order(String userId, UUID checkoutId, Product product, ProductType type, int quantity) {
        this.userId = userId;
        this.checkoutId = checkoutId;
        this.productId = product.getId();
        this.productTypeId = type.getId();
        this.productName = product.getName();
        this.productTypeName = type.getName();
        this.productSubtitle = type.getIntro() == null || type.getIntro().isBlank() ? product.getSubtitle() : type.getIntro();
        this.quantity = quantity;
        this.amount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        this.status = OrderStatus.PENDING_PAYMENT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public UUID getCheckoutId() {
        return checkoutId;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductTypeId() {
        return productTypeId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductTypeName() {
        return productTypeName;
    }

    public String getProductSubtitle() {
        return productSubtitle;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public String getExpressCompany() {
        return expressCompany;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public Instant getShippingUploadedAt() {
        return shippingUploadedAt;
    }

    public boolean canRequestAfterSale() {
        return status == OrderStatus.PAID
                || status == OrderStatus.PROCESSING
                || status == OrderStatus.COMPLETED;
    }

    public void markRefunding() {
        if (!canRequestAfterSale()) {
            throw new IllegalStateException("当前订单状态不支持退款/售后");
        }
        status = OrderStatus.REFUNDING;
        updatedAt = Instant.now();
    }

    public void markPaid() {
        if (status == OrderStatus.PAID || status == OrderStatus.PROCESSING || status == OrderStatus.COMPLETED) {
            return;
        }
        if (status != OrderStatus.PENDING_PAYMENT && status != OrderStatus.CANCELLED) {
            throw new IllegalStateException("Only pending orders can be paid");
        }
        status = OrderStatus.PAID;
        updatedAt = Instant.now();
    }

    public void markProcessing() {
        if (status == OrderStatus.PROCESSING) return;
        if (status != OrderStatus.PAID) throw new IllegalStateException("Only paid orders can be shipped");
        status = OrderStatus.PROCESSING;
        updatedAt = Instant.now();
    }

    public void ship(String trackingNo, String expressCompany) {
        markProcessing();
        this.trackingNo = trackingNo == null ? null : trackingNo.trim();
        this.expressCompany = expressCompany == null ? null : expressCompany.trim();
        this.shippedAt = Instant.now();
        updatedAt = this.shippedAt;
    }

    public void markShippingUploaded() {
        this.shippingUploadedAt = Instant.now();
        updatedAt = this.shippingUploadedAt;
    }

    public void restoreFromRefunding() {
        if (status != OrderStatus.REFUNDING) throw new IllegalStateException("当前订单不是售后中");
        status = shippedAt != null ? OrderStatus.PROCESSING : OrderStatus.PAID;
        updatedAt = Instant.now();
    }

    public void markRefunded() {
        if (status != OrderStatus.REFUNDING && status != OrderStatus.REFUNDED) {
            throw new IllegalStateException("当前订单状态不支持完成退款");
        }
        status = OrderStatus.REFUNDED;
        updatedAt = Instant.now();
    }

    public void cancel() {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }
        status = OrderStatus.CANCELLED;
        updatedAt = Instant.now();
    }
}
