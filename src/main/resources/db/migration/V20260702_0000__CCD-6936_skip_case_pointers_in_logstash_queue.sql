-- Limit how long this migration waits for locks to ensure we do not hang if long queries are active.
SET LOCAL lock_timeout = '15s';

ALTER TABLE public.case_data
    DROP CONSTRAINT IF EXISTS case_pointer_always_marked_by_logstash;

CREATE OR REPLACE FUNCTION public.insert_update_logstash_queue () RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT (NEW.data = '{}'::jsonb AND NEW.state = '') THEN
        INSERT INTO public.case_data_logstash_queue ("case_data_id") VALUES (NEW.id);
    END IF;
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION public.insert_update_logstash_queue() IS
    'Adds changed case rows to the Logstash queue, excluding case pointer rows.';
