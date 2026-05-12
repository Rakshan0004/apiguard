-- Add index on disabled_reason for efficient querying of disabled keys
CREATE INDEX idx_api_keys_disabled_reason 
    ON api_keys(disabled_reason) 
    WHERE disabled_reason IS NOT NULL;
