CREATE TABLE coupon_template_products (
    template_id BINARY(16) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (template_id, product_id),
    CONSTRAINT fk_coupon_template_products_template FOREIGN KEY (template_id) REFERENCES coupon_templates (id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_template_products_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO announcements (id, title, content, published, created_at, updated_at)
VALUES (
    UNHEX(REPLACE('44444444-4444-4444-4444-444444444444', '-', '')),
    '优惠券使用范围',
    '部分商品毛利率较低，不支持使用无门槛券。
1. 优惠券只能用于指定商品。未指定的商品不计入门槛，也不能被这张券抵扣。
2. 无门槛券同样只对指定商品生效，毛利率较低的商品不在范围内。
3. 同一订单里如果同时有可用和不可用商品，只按可用商品金额判断是否满足门槛。
4. 具体可用商品以优惠券和结算页说明为准。',
    1,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);
