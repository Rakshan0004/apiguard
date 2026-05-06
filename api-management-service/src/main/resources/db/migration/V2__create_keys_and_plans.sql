CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    rate_limit_rpm INT NOT NULL,
    monthly_quota BIGINT NOT NULL,
    webhook_enabled BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    key_prefix VARCHAR(8) NOT NULL,
    registered_api_id UUID NOT NULL REFERENCES registered_apis(id),
    plan_id UUID NOT NULL REFERENCES plans(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    disabled_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed initial plans
INSERT INTO plans (name, rate_limit_rpm, monthly_quota, webhook_enabled) VALUES
('FREE', 10, 1000, FALSE),
('BASIC', 60, 50000, FALSE),
('PRO', 300, 500000, TRUE),
('ENTERPRISE', 1000, 5000000, TRUE);
