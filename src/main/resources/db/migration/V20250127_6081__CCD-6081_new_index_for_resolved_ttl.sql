CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_resolved_ttl ON case_data(resolved_ttl) where resolved_ttl is not null;
