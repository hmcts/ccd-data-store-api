-- Stop all Logstash consumers before applying this migration. Verify that every
-- destination ES index (including global_search) has versions below 10^10.
SET LOCAL lock_timeout = '15s';

LOCK TABLE public.case_data_logstash_queue IN ACCESS EXCLUSIVE MODE;
ALTER TABLE public.case_data_logstash_queue ALTER COLUMN id TYPE bigint;
ALTER SEQUENCE public.case_data_logstash_queue_id_seq AS bigint;

-- RESTART is transactional, unlike setval. Never rewind an existing sequence,
-- including values consumed by rolled-back inserts or ON CONFLICT DO NOTHING.
DO $$
DECLARE
    next_id bigint;
BEGIN
    SELECT GREATEST(10000000000::bigint,
                    (SELECT last_value FROM public.case_data_logstash_queue_id_seq),
                    COALESCE((SELECT max(id) FROM public.case_data_logstash_queue), 0)) + 1
    INTO next_id;
    EXECUTE format('ALTER SEQUENCE public.case_data_logstash_queue_id_seq RESTART WITH %s', next_id);
END;
$$;

-- Raising the sequence alone leaves the pre-cutover backlog with unsafe IDs.
-- Preserve every queued case and allocate IDs above all previous queue IDs.
UPDATE public.case_data_logstash_queue
SET id = nextval('public.case_data_logstash_queue_id_seq');
