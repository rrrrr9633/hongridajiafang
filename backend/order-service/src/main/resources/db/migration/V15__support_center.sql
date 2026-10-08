CREATE TABLE support_settings (
    id VARCHAR(32) NOT NULL,
    wechat_id VARCHAR(64) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO support_settings (id, wechat_id) VALUES ('default', NULL);

CREATE TABLE support_messages (
    id BINARY(16) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    sender VARCHAR(16) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    read_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY idx_support_user_created (user_id, created_at),
    KEY idx_support_user_unread (user_id, sender, read_at),
    CONSTRAINT fk_support_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
