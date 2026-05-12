CREATE TABLE usage_logs (
    id VARCHAR(36) PRIMARY KEY,
    api_key_id VARCHAR(255) NOT NULL,
    api_id VARCHAR(255) NOT NULL,
    method VARCHAR(10),
    path TEXT,
    status INT,
    latency_ms BIGINT,
    timestamp TIMESTAMP NOT NULL
);

CREATE TABLE monthly_usage_summaries (
    id SERIAL PRIMARY KEY,
    api_key_id VARCHAR(255) NOT NULL,
    year_month VARCHAR(7) NOT NULL,
    total_requests BIGINT DEFAULT 0,
    successful_requests BIGINT DEFAULT 0,
    UNIQUE (api_key_id, year_month)
);

CREATE INDEX idx_usage_logs_api_key_id ON usage_logs(api_key_id);
CREATE INDEX idx_usage_logs_timestamp ON usage_logs(timestamp);
