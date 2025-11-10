-- Ensure case pointer rows keep their marked_by_logstash flag
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
