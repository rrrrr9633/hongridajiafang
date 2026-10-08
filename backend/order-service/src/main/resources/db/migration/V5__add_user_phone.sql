SET @phone_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'phone'
);

SET @add_phone_sql = IF(
    @phone_column_exists = 0,
    'ALTER TABLE users ADD COLUMN phone VARCHAR(32) NULL',
    'SELECT 1'
);

PREPARE add_phone_statement FROM @add_phone_sql;
EXECUTE add_phone_statement;
DEALLOCATE PREPARE add_phone_statement;

UPDATE users
SET phone = CONCAT('legacy-', LEFT(SHA2(id, 256), 25))
WHERE phone IS NULL;

ALTER TABLE users
    MODIFY COLUMN phone VARCHAR(32) NOT NULL;

SET @phone_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND index_name = 'uk_users_phone'
);

SET @add_phone_index_sql = IF(
    @phone_index_exists = 0,
    'CREATE UNIQUE INDEX uk_users_phone ON users (phone)',
    'SELECT 1'
);

PREPARE add_phone_index_statement FROM @add_phone_index_sql;
EXECUTE add_phone_index_statement;
DEALLOCATE PREPARE add_phone_index_statement;
