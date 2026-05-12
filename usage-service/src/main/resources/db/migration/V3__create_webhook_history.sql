-- Create webhook_history table for tracking webhook delivery attempts
CREATE TABLE webhook_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    api_key_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    threshold_percentage INT NOT NULL,
    year_month VARCHAR(7) NOT NULL,
    usage_count BIGINT NOT NULL,
    quota_limit BIGINT NOT NULL,
    usage_percentage DECIMAL(5,2) NOT NULL,
    webhook_url VARCHAR(2048) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    http_status_code INT,
    retry_count INT NOT NULL DEFAULT 0,
    delivery_status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create unique index for deduplication (prevents duplicate notifications for same threshold in same month)
CREATE UNIQUE INDEX idx_webhook_history_dedup ON webhook_history(api_key_id, event_type, year_month);

-- Create index for history retrieval (ordered by most recent first)
CREATE INDEX idx_webhook_history_api_key_sent ON webhook_history(api_key_id, sent_at DESC);

-- Create index for monitoring queries (find failed deliveries)
CREATE INDEX idx_webhook_history_status_sent ON webhook_history(delivery_status, sent_at DESC);

-- Add comments for documentation
COMMENT ON TABLE webhook_history IS 'Tracks all webhook delivery attempts with status and error details';
COMMENT ON COLUMN webhook_history.event_type IS 'Type of event: quota.warning, quota.exceeded, quota.test';
COMMENT ON COLUMN webhook_history.threshold_percentage IS 'Threshold that triggered notification: 80 or 100';
COMMENT ON COLUMN webhook_history.year_month IS 'Billing period in YYYY-MM format';
COMMENT ON COLUMN webhook_history.delivery_status IS 'Final delivery status: SUCCESS or FAILED';
COMMENT ON COLUMN webhook_history.retry_count IS 'Number of retry attempts (0-3)';
