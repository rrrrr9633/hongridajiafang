ALTER TABLE coupon_templates
    ADD COLUMN per_order_limit INT NOT NULL DEFAULT 1;
