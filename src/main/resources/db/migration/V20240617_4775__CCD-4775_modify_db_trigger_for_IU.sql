DROP FUNCTION IF exists insert_update_logstash_queue;

CREATE OR REPLACE FUNCTION public.insert_update_logstash_queue () RETURNS TRIGGER
    LANGUAGE plpgsql
    AS $$
    BEGIN
        INSERT INTO public.case_data_logstash_queue ("case_data_id") VALUES (NEW.id);
        RETURN NEW;
    END;
    $$;

DROP TRIGGER IF EXISTS trg_case_data_updated_queue ON public.case_data;

CREATE TRIGGER trg_case_data_updated_queue
BEFORE INSERT OR UPDATE OF data, data_classification, last_modified, last_state_modified_date, security_classification, state, supplementary_data
ON public.case_data
FOR EACH ROW EXECUTE FUNCTION public.insert_update_logstash_queue();



