ALTER TABLE orders
    ADD COLUMN tracking_no VARCHAR(64) NULL,
    ADD COLUMN express_company VARCHAR(32) NULL,
    ADD COLUMN shipped_at TIMESTAMP(6) NULL,
    ADD COLUMN shipping_uploaded_at TIMESTAMP(6) NULL;

ALTER TABLE after_sales
    ADD COLUMN previous_status VARCHAR(32) NULL,
    ADD COLUMN out_refund_no VARCHAR(64) NULL,
    ADD COLUMN wechat_refund_id VARCHAR(64) NULL,
    ADD UNIQUE KEY uk_after_sales_out_refund (out_refund_no);
