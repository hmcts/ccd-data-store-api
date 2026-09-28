ALTER TABLE public.case_data_logstash_queue
    ADD COLUMN IF NOT EXISTS claim_token text,
    ADD COLUMN IF NOT EXISTS claimed_at timestamptz;

CREATE INDEX IF NOT EXISTS idx_case_data_logstash_queue_claimed_at
    ON public.case_data_logstash_queue (claimed_at);
