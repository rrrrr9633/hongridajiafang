INSERT INTO products (id, name, subtitle, price, original_price, color, accent, pattern, tag, active)
VALUES
    ('bamboo-spring', '春日竹语四件套', '天丝竹纤维 · 1.5米床', 399.00, 499.00, '#d9e5d8', '#6d896e', '竹', '轻柔透气', TRUE),
    ('linen-moon', '月白亚麻床笠', '水洗亚麻 · 纯色款', 269.00, 329.00, '#e5ddd2', '#9b8871', '月', '自然肌理', TRUE),
    ('cotton-cloud', '云朵全棉被套', '新疆长绒棉 · 亲肤款', 329.00, 399.00, '#dce8ed', '#668b9a', '云', '柔软亲肤', TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    subtitle = VALUES(subtitle),
    price = VALUES(price),
    original_price = VALUES(original_price),
    color = VALUES(color),
    accent = VALUES(accent),
    pattern = VALUES(pattern),
    tag = VALUES(tag),
    active = VALUES(active);
