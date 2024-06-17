CREATE OR REPLACE FUNCTION insert_logstash_queue ()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.case_data_logstash_queue ("case_data_id") VALUES (NEW.id);
    RETURN NEW;
END;

$$ LANGUAGE plpgsql;-- drop trigger from case_data and add new one
DROP TRIGGER IF EXISTS trg_case_data_updated ON public.case_data;

CREATE TRIGGER trg_case_data_updated
BEFORE INSERT OR UPDATE OF data, data_classification, last_modified, last_state_modified_date, security_classification, state, supplementary_data
ON public.case_data
FOR EACH ROW EXECUTE PROCEDURE public.insert_logstash_queue();

