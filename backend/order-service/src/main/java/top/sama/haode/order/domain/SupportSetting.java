package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "support_settings")
public class SupportSetting {
    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "wechat_id", length = 64)
    private String wechatId;

    protected SupportSetting() {
    }

    public SupportSetting(String id) {
        this.id = id;
    }

    public String getWechatId() {
        return wechatId;
    }

    public void setWechatId(String wechatId) {
        this.wechatId = wechatId == null || wechatId.isBlank() ? null : wechatId.trim();
    }
}
