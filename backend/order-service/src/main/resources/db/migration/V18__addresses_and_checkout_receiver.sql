CREATE TABLE user_addresses (
    id BINARY(16) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    receiver_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    province VARCHAR(64) NOT NULL,
    city VARCHAR(64) NOT NULL,
    district VARCHAR(64) NOT NULL,
    detail VARCHAR(255) NOT NULL,
    is_default BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_addresses_user_default (user_id, is_default),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE checkouts
    ADD COLUMN address_id BINARY(16) NULL,
    ADD COLUMN receiver_name VARCHAR(64) NULL,
    ADD COLUMN receiver_phone VARCHAR(32) NULL,
    ADD COLUMN receiver_province VARCHAR(64) NULL,
    ADD COLUMN receiver_city VARCHAR(64) NULL,
    ADD COLUMN receiver_district VARCHAR(64) NULL,
    ADD COLUMN receiver_detail VARCHAR(255) NULL;
