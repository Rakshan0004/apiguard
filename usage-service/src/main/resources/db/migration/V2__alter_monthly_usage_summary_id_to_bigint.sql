ALTER TABLE monthly_usage_summaries
    ALTER COLUMN id TYPE BIGINT;

ALTER SEQUENCE monthly_usage_summaries_id_seq
    AS BIGINT;
