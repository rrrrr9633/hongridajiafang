CREATE TABLE coupon_usage_rules (
    id BINARY(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    priority INT NOT NULL,
    zones_json LONGTEXT NOT NULL,
    product_ids_json LONGTEXT NOT NULL,
    options_json LONGTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE announcements
SET title = '优惠券使用规则',
    content = '优惠券不再按每张券指定商品，也不再是全场统一的每单张数。结算时按商品规则用券。
1. 一单只执行优先级数字最小、且命中了本单商品的规则。数字更大的规则一律忽略。
2. 优先级相同的规则只能满足其中一条，不能叠加。不用手动选择，勾选时能放下才允许，放不下就不能再勾。
3. 一条规则里的多种用法也是二选一。例如床品区加指定袜子：1 张 10 元券，或 3 张 5 元券，或 1 张 5 元券加 1 张 10 元券。用得更少可以，不能把几种用法加在一起。
4. 订单里只要有一件商品命中规则，整单都按这条规则用券。没有任何规则命中时不能用券。
5. 满减门槛按整单商品金额计算。',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE id = UNHEX(REPLACE('44444444-4444-4444-4444-444444444444', '-', ''));
