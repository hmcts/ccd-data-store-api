-- drop INDEX idx_case_data_marked_by_logstash
-- remove column marked_by_logstash from case_data
-- as now replaced with case_data_logstash_queue table

DROP INDEX IF exists idx_case_data_marked_by_logstash;
ALTER table case_data
DROP COLUMN IF EXISTS marked_by_logstash


