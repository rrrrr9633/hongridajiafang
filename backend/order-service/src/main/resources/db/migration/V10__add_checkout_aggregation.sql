CREATE TABLE checkouts (
    id BINARY(16) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_checkouts_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE orders
    ADD COLUMN checkout_id BINARY(16) NULL,
    ADD KEY idx_orders_checkout (checkout_id),
    ADD CONSTRAINT fk_orders_checkout FOREIGN KEY (checkout_id) REFERENCES checkouts (id);

ALTER TABLE payments
    DROP FOREIGN KEY fk_payments_order,
    DROP INDEX uk_payments_order,
    MODIFY COLUMN order_id BINARY(16) NULL,
    ADD COLUMN checkout_id BINARY(16) NULL,
    ADD UNIQUE KEY uk_payments_checkout (checkout_id),
    ADD CONSTRAINT fk_payments_checkout FOREIGN KEY (checkout_id) REFERENCES checkouts (id);

INSERT INTO checkouts (id, user_id, amount, status, created_at, updated_at)
SELECT id, user_id, amount, status, created_at, updated_at
FROM orders;

UPDATE orders
SET checkout_id = id
WHERE checkout_id IS NULL;

UPDATE payments p
JOIN checkouts c ON c.id = p.order_id
SET p.checkout_id = c.id;

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id);
