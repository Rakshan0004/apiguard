CREATE TABLE registered_apis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    owner_email VARCHAR(255) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    proxy_path VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_apis_proxy_path ON registered_apis(proxy_path);
