package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_addresses")
public class UserAddress {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "receiver_name", nullable = false, length = 64)
    private String receiverName;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(nullable = false, length = 64)
    private String province;

    @Column(nullable = false, length = 64)
    private String city;

    @Column(nullable = false, length = 64)
    private String district;

    @Column(nullable = false, length = 255)
    private String detail;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAddress() {
    }

    public UserAddress(String userId, String receiverName, String phone, String province, String city, String district, String detail, boolean defaultAddress) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.createdAt = Instant.now();
        replace(receiverName, phone, province, city, district, detail, defaultAddress);
    }

    public void replace(String receiverName, String phone, String province, String city, String district, String detail, boolean defaultAddress) {
        this.receiverName = requireText(receiverName, "收货人");
        this.phone = requireText(phone, "手机号");
        this.province = requireText(province, "省份");
        this.city = requireText(city, "城市");
        this.district = requireText(district, "区县");
        this.detail = requireText(detail, "详细地址");
        this.defaultAddress = defaultAddress;
        this.updatedAt = Instant.now();
    }

    public void markDefault(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
        this.updatedAt = Instant.now();
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("请填写" + label);
        return value.trim();
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getReceiverName() { return receiverName; }
    public String getPhone() { return phone; }
    public String getProvince() { return province; }
    public String getCity() { return city; }
    public String getDistrict() { return district; }
    public String getDetail() { return detail; }
    public boolean isDefaultAddress() { return defaultAddress; }

    public String line() {
        return province + city + district + detail;
    }
}
