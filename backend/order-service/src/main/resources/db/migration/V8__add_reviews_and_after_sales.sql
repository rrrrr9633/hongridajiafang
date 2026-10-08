CREATE TABLE reviews (
    id BINARY(16) NOT NULL,
    order_id BINARY(16) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    rating TINYINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reviews_order (order_id),
    KEY idx_reviews_user_created (user_id, created_at),
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE after_sales (
    id BINARY(16) NOT NULL,
    order_id BINARY(16) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_after_sales_order (order_id),
    KEY idx_after_sales_user_created (user_id, created_at),
    CONSTRAINT fk_after_sales_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
