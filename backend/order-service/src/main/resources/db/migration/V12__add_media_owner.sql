ALTER TABLE media_assets
    ADD COLUMN owner_id VARCHAR(64) NULL,
    ADD KEY idx_media_assets_owner (owner_id);
