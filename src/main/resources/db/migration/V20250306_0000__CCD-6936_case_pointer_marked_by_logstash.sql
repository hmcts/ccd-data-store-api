CREATE OR REPLACE FUNCTION public.set_case_data_marked_by_logstash() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.data = '{}'::jsonb AND NEW.state = '' THEN
        NEW.marked_by_logstash := true;
    ELSE
        NEW.marked_by_logstash := false;
    END IF;
    RETURN NEW;
END
$$;

COMMENT ON FUNCTION public.set_case_data_marked_by_logstash() IS
    'Ensures case pointer rows (empty data/state) always remain marked_by_logstash=true while case records are flagged unmarked on write.';

ALTER TABLE case_data
    ADD CONSTRAINT case_pointer_always_marked_by_logstash
    CHECK (
        marked_by_logstash
        or NOT (data = '{}'::jsonb AND state = '')
    )
    NOT VALID;

COMMENT ON CONSTRAINT case_pointer_always_marked_by_logstash ON case_data IS
    'Ensure case pointers (empty data & state) cannot be set marked_by_logstash=false; NOT VALID avoids ACCESS EXCLUSIVE lock and full table scan but the check applies to new updates.';
