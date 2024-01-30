CREATE TABLE IF NOT EXISTS public.case_data_logstash_queue (
    id serial PRIMARY KEY,
    case_data_id bigint NOT NULL,
    FOREIGN KEY (case_data_id) REFERENCES public.case_data (id) ON DELETE CASCADE
);
