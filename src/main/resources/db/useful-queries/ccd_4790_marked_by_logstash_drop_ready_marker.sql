-- One-off CCD-4790 release marker.
--
-- Run this only after:
-- 1. logstash_re_indexing_query.sql has queued the required rows.
-- 2. case_data_logstash_queue has drained.
-- 3. Elasticsearch _count results match logstash_re_indexing_validation_query.sql.
-- 4. Sampled Elasticsearch _doc lookups return the expected documents.

CREATE TABLE IF NOT EXISTS public.ccd_data_migration_status (
    migration_name text PRIMARY KEY,
    completed_at timestamp with time zone NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.case_data_logstash_queue LIMIT 1) THEN
        RAISE EXCEPTION
            'CCD-4790 marker blocked: case_data_logstash_queue is not empty';
    END IF;
END $$;

INSERT INTO public.ccd_data_migration_status (migration_name, completed_at)
VALUES ('CCD-4790-marked-by-logstash-drop-ready', now())
ON CONFLICT (migration_name) DO UPDATE
SET completed_at = EXCLUDED.completed_at;
