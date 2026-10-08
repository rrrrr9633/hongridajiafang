package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invite_purchase_rewards")
public class InvitePurchaseReward {
    @Id
    @Column(name = "invitee_id", length = 64)
    private String inviteeId;

    @Column(name = "inviter_id", nullable = false, length = 64)
    private String inviterId;

    @Column(name = "coupon_id")
    private UUID couponId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InvitePurchaseReward() {
    }

    public InvitePurchaseReward(String inviteeId, String inviterId, UUID couponId) {
        this.inviteeId = inviteeId;
        this.inviterId = inviterId;
        this.couponId = couponId;
        this.createdAt = Instant.now();
    }
}
