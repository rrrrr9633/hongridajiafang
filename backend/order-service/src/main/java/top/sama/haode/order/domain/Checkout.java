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
@Table(name = "checkouts")
public class Checkout {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "goods_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal goodsAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "coupon_id")
    private UUID couponId;

    @Column(name = "address_id")
    private UUID addressId;

    @Column(name = "receiver_name", length = 64)
    private String receiverName;

    @Column(name = "receiver_phone", length = 32)
    private String receiverPhone;

    @Column(name = "receiver_province", length = 64)
    private String receiverProvince;

    @Column(name = "receiver_city", length = 64)
    private String receiverCity;

    @Column(name = "receiver_district", length = 64)
    private String receiverDistrict;

    @Column(name = "receiver_detail", length = 255)
    private String receiverDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Checkout() {}

    public Checkout(String userId, BigDecimal amount) {
        this(UUID.randomUUID(), userId, amount);
    }

    public Checkout(UUID id, String userId, BigDecimal amount) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.goodsAmount = amount;
        this.discountAmount = BigDecimal.ZERO;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("结算金额无效");
        this.goodsAmount = amount;
        this.discountAmount = BigDecimal.ZERO;
        this.couponId = null;
        this.amount = amount;
        this.updatedAt = Instant.now();
    }

    public void applyDiscount(BigDecimal discountAmount) {
        if (status != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("结算单当前状态不可用券");
        if (discountAmount == null || discountAmount.signum() < 0) throw new IllegalArgumentException("优惠金额无效");
        if (discountAmount.signum() == 0) {
            clearCoupon();
            return;
        }
        BigDecimal payable = goodsAmount.subtract(discountAmount);
        if (payable.signum() <= 0) throw new IllegalArgumentException("优惠后金额无效，请减少用券数量");
        this.discountAmount = discountAmount;
        this.amount = payable;
        this.updatedAt = Instant.now();
    }

    public void rememberCoupon(UUID couponId) {
        this.couponId = couponId;
    }

    public void clearCoupon() {
        this.couponId = null;
        this.discountAmount = BigDecimal.ZERO;
        this.amount = goodsAmount;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getGoodsAmount() { return goodsAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void bindAddress(UserAddress address) {
        if (status != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("结算单当前状态不可修改地址");
        if (address == null) throw new IllegalArgumentException("请选择收货地址");
        this.addressId = address.getId();
        this.receiverName = address.getReceiverName();
        this.receiverPhone = address.getPhone();
        this.receiverProvince = address.getProvince();
        this.receiverCity = address.getCity();
        this.receiverDistrict = address.getDistrict();
        this.receiverDetail = address.getDetail();
        this.updatedAt = Instant.now();
    }

    public boolean hasReceiver() {
        return receiverName != null && !receiverName.isBlank()
                && receiverPhone != null && !receiverPhone.isBlank()
                && receiverDetail != null && !receiverDetail.isBlank();
    }

    public UUID getCouponId() { return couponId; }
    public UUID getAddressId() { return addressId; }
    public String getReceiverName() { return receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public String getReceiverProvince() { return receiverProvince; }
    public String getReceiverCity() { return receiverCity; }
    public String getReceiverDistrict() { return receiverDistrict; }
    public String getReceiverDetail() { return receiverDetail; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) return;
        if (status != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("结算单当前状态不可取消");
        status = OrderStatus.CANCELLED;
        updatedAt = Instant.now();
    }

    public void markPaid() {
        if (status != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("结算单当前状态不可支付");
        status = OrderStatus.PAID;
        updatedAt = Instant.now();
    }
}
