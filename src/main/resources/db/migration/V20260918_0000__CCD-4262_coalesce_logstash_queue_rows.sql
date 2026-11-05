-- Coalesce existing backlog to one fresh, monotonic queue event per case before
-- enforcing the one-outstanding-row queue contract.
SET LOCAL lock_timeout = '15s';

WITH removed_queue_rows AS (
    DELETE FROM public.case_data_logstash_queue
    RETURNING case_data_id
)
INSERT INTO public.case_data_logstash_queue (case_data_id)
SELECT DISTINCT removed.case_data_id
FROM removed_queue_rows removed
JOIN public.case_data cd ON cd.id = removed.case_data_id
WHERE NOT (cd.data = '{}'::jsonb AND cd.state = '');

ALTER TABLE public.case_data_logstash_queue
    ADD CONSTRAINT case_data_logstash_queue_case_data_id_unique UNIQUE (case_data_id);

CREATE OR REPLACE FUNCTION public.insert_update_logstash_queue () RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT (NEW.data = '{}'::jsonb AND NEW.state = '') THEN
        INSERT INTO public.case_data_logstash_queue (case_data_id)
        VALUES (NEW.id)
        ON CONFLICT (case_data_id) DO NOTHING;
    END IF;
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION public.insert_update_logstash_queue() IS
    'Queues one outstanding Logstash event per changed non-pointer case.';
