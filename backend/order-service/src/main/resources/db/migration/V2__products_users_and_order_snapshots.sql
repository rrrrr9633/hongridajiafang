CREATE TABLE products (
    id VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    subtitle VARCHAR(255) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    original_price DECIMAL(12, 2) NULL,
    color VARCHAR(32) NULL,
    accent VARCHAR(32) NULL,
    pattern VARCHAR(64) NULL,
    tag VARCHAR(64) NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_products_active_id (active, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id VARCHAR(64) NOT NULL,
    wechat_openid VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_wechat_openid (wechat_openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sessions (
    token_hash CHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (token_hash),
    KEY idx_sessions_user (user_id),
    KEY idx_sessions_expiry (expires_at),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE orders
    ADD COLUMN product_id VARCHAR(64) NULL,
    ADD COLUMN product_name VARCHAR(128) NULL,
    ADD COLUMN product_subtitle VARCHAR(255) NULL,
    ADD COLUMN quantity INT NOT NULL DEFAULT 1;

ALTER TABLE orders
    MODIFY COLUMN product_id VARCHAR(64) NOT NULL,
    MODIFY COLUMN product_name VARCHAR(128) NOT NULL,
    MODIFY COLUMN product_subtitle VARCHAR(255) NOT NULL,
    ADD KEY idx_orders_product (product_id),
    ADD CONSTRAINT fk_orders_product FOREIGN KEY (product_id) REFERENCES products (id);
