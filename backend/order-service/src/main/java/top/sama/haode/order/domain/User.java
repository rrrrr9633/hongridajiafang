package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "wechat_openid", nullable = false, unique = true, length = 128)
    private String wechatOpenid;

    @Column(name = "phone", nullable = false, unique = true, length = 32)
    private String phone;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String id, String wechatOpenid, String phone) {
        this.id = id;
        this.wechatOpenid = wechatOpenid;
        this.phone = phone;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public User bindWechatOpenid(String openid) {
        if (!wechatOpenid.equals(openid)) {
            this.wechatOpenid = openid;
            this.updatedAt = Instant.now();
        }
        return this;
    }

    public String getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void updateAvatar(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
