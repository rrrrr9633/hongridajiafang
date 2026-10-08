ALTER TABLE users
    ADD COLUMN invite_code VARCHAR(16) NULL,
    ADD COLUMN inviter_id VARCHAR(64) NULL;

UPDATE users
SET invite_code = UPPER(SUBSTRING(REPLACE(id, '-', ''), 1, 8))
WHERE invite_code IS NULL;

ALTER TABLE users
    MODIFY invite_code VARCHAR(16) NOT NULL,
    ADD UNIQUE KEY uk_users_invite_code (invite_code),
    ADD KEY idx_users_inviter (inviter_id);

ALTER TABLE checkouts
    ADD COLUMN goods_amount DECIMAL(12, 2) NULL,
    ADD COLUMN discount_amount DECIMAL(12, 2) NULL,
    ADD COLUMN coupon_id BINARY(16) NULL;

UPDATE checkouts
SET goods_amount = amount,
    discount_amount = 0
WHERE goods_amount IS NULL;

ALTER TABLE checkouts
    MODIFY goods_amount DECIMAL(12, 2) NOT NULL,
    MODIFY discount_amount DECIMAL(12, 2) NOT NULL;

CREATE TABLE coupon_templates (
    id BINARY(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    threshold_amount DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO coupon_templates (id, name, threshold_amount, discount_amount, active, created_at, updated_at)
VALUES
    (UNHEX(REPLACE('11111111-1111-1111-1111-111111111111', '-', '')), '邀请礼券', 0.00, 10.00, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE('22222222-2222-2222-2222-222222222222', '-', '')), '消费奖励券', 0.00, 20.00, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

CREATE TABLE coupon_policies (
    id VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL,
    invite_template_id BINARY(16) NULL,
    purchase_template_id BINARY(16) NULL,
    purchase_threshold DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_policy_invite_template FOREIGN KEY (invite_template_id) REFERENCES coupon_templates (id),
    CONSTRAINT fk_policy_purchase_template FOREIGN KEY (purchase_template_id) REFERENCES coupon_templates (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO coupon_policies (id, enabled, invite_template_id, purchase_template_id, purchase_threshold)
VALUES (
    'default',
    1,
    UNHEX(REPLACE('11111111-1111-1111-1111-111111111111', '-', '')),
    UNHEX(REPLACE('22222222-2222-2222-2222-222222222222', '-', '')),
    200.00
);

CREATE TABLE user_coupons (
    id BINARY(16) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    template_id BINARY(16) NULL,
    name VARCHAR(64) NOT NULL,
    threshold_amount DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) NOT NULL,
    source VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    checkout_id BINARY(16) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    used_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY idx_user_coupons_user_status (user_id, status, created_at),
    KEY idx_user_coupons_checkout (checkout_id),
    CONSTRAINT fk_user_coupons_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_coupons_template FOREIGN KEY (template_id) REFERENCES coupon_templates (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invite_purchase_rewards (
    invitee_id VARCHAR(64) NOT NULL,
    inviter_id VARCHAR(64) NOT NULL,
    coupon_id BINARY(16) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (invitee_id),
    KEY idx_invite_rewards_inviter (inviter_id),
    CONSTRAINT fk_invite_rewards_invitee FOREIGN KEY (invitee_id) REFERENCES users (id),
    CONSTRAINT fk_invite_rewards_inviter FOREIGN KEY (inviter_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE announcements (
    id BINARY(16) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    published BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_announcements_published_created (published, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO announcements (id, title, content, published, created_at, updated_at)
VALUES (
    UNHEX(REPLACE('33333333-3333-3333-3333-333333333333', '-', '')),
    '优惠券活动',
    '即日起开放邀请有礼：\n1. 在「我的优惠券」填写好友邀请码，双方各得一张邀请礼券（优惠券1）。每个账号只能绑定一次上级。\n2. 每成功邀请一位好友，邀请人都会再得一张优惠券1。\n3. 被邀请人累计实付达到后台设定金额后，邀请人再得一张消费奖励券（优惠券2）。\n4. 邀请只计直属一层，好友再邀请的人不算在你名下。\n5. 下单时每笔结算最多使用一张优惠券；满0减X为无门槛券。\n\n具体满减金额以优惠券页和后台当前设置为准。',
    1,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);

CREATE TABLE announcement_reads (
    announcement_id BINARY(16) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    read_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (announcement_id, user_id),
    KEY idx_announcement_reads_user (user_id),
    CONSTRAINT fk_announcement_reads_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT fk_announcement_reads_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
