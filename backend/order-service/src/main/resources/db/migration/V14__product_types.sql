CREATE TABLE product_types (
    id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    intro VARCHAR(255) NOT NULL,
    image_url VARCHAR(1024) NULL,
    image_asset_id BINARY(16) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    KEY idx_product_types_product (product_id, sort_order, id),
    CONSTRAINT fk_product_types_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_types_image_asset FOREIGN KEY (image_asset_id) REFERENCES media_assets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO product_types (id, product_id, name, intro, image_url, image_asset_id, sort_order, active)
SELECT CONCAT(id, '-default'), id, '默认款', subtitle, image_url, image_asset_id, 0, TRUE
FROM products;

ALTER TABLE orders
    ADD COLUMN product_type_id VARCHAR(64) NULL AFTER product_id,
    ADD COLUMN product_type_name VARCHAR(64) NULL AFTER product_name;
