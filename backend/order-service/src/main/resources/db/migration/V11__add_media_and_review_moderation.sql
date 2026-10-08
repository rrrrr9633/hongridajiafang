CREATE TABLE media_assets (
    id BINARY(16) NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE products
    ADD COLUMN image_asset_id BINARY(16) NULL,
    ADD CONSTRAINT fk_products_image_asset FOREIGN KEY (image_asset_id) REFERENCES media_assets (id);

ALTER TABLE reviews
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    ADD KEY idx_reviews_status_created (status, created_at);

CREATE TABLE review_images (
    id BINARY(16) NOT NULL,
    review_id BINARY(16) NOT NULL,
    asset_id BINARY(16) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_images_asset (review_id, asset_id),
    KEY idx_review_images_review_sort (review_id, sort_order),
    CONSTRAINT fk_review_images_review FOREIGN KEY (review_id) REFERENCES reviews (id),
    CONSTRAINT fk_review_images_asset FOREIGN KEY (asset_id) REFERENCES media_assets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
