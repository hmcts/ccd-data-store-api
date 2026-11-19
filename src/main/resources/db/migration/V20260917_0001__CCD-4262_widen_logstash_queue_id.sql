-- Widen before coalescing allocates replacement IDs near the int4 limit.
-- Also safe for environments applying this out of order after the bigint upgrade.
SET LOCAL lock_timeout = '15s';

ALTER TABLE public.case_data_logstash_queue ALTER COLUMN id TYPE bigint;
ALTER SEQUENCE public.case_data_logstash_queue_id_seq AS bigint;
