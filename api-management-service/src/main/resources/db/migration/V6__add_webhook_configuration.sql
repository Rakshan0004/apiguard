-- Add webhook configuration columns to api_keys table
ALTER TABLE api_keys ADD COLUMN webhook_url VARCHAR(2048);
ALTER TABLE api_keys ADD COLUMN webhook_secret VARCHAR(64);

-- Add comment for documentation
COMMENT ON COLUMN api_keys.webhook_url IS 'HTTPS endpoint for webhook notifications (nullable)';
COMMENT ON COLUMN api_keys.webhook_secret IS 'Shared secret for HMAC signature generation (nullable)';
