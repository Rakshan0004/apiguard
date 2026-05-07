-- Delete existing data to avoid constraint violations during refactor
DELETE FROM api_keys;
DELETE FROM plans;

-- Remove unique constraint from name (plans will be unique per API now)
ALTER TABLE plans DROP CONSTRAINT IF EXISTS plans_name_key;

-- Add multi-tenant columns
ALTER TABLE plans ADD COLUMN owner_email VARCHAR(255) NOT NULL;
ALTER TABLE plans ADD COLUMN api_id UUID NOT NULL REFERENCES registered_apis(id);

-- Optional: Add a composite unique constraint to ensure plan names are unique per API
ALTER TABLE plans ADD CONSTRAINT unique_plan_name_per_api UNIQUE (api_id, name);
