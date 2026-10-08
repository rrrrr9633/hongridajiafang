-- V2 创建的 sessions.token_hash 与 JPA 映射保持一致，避免新数据库启动时校验失败。
ALTER TABLE sessions
    MODIFY COLUMN token_hash VARCHAR(64) NOT NULL;
