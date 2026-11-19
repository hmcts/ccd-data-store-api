DROP INDEX IF EXISTS public.idx_case_data_logstash_queue_claimed_at;

ALTER TABLE public.case_data_logstash_queue
    DROP COLUMN IF EXISTS claim_token,
    DROP COLUMN IF EXISTS claimed_at;
