ALTER TABLE products
    ADD COLUMN zone VARCHAR(64) NOT NULL DEFAULT '床品区' AFTER tag,
    ADD COLUMN homepage BOOLEAN NOT NULL DEFAULT FALSE AFTER zone,
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 AFTER homepage;

UPDATE products SET zone = '床品区', homepage = TRUE, sort_order = 0;
