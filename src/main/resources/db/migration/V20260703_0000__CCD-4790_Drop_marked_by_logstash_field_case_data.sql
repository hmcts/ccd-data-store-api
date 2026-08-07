-- Drop legacy marked_by_logstash state after Logstash has moved to case_data_logstash_queue.
-- The completion marker records that the one-off readiness checks have been completed. The
-- database cannot independently prove Logstash has drained the queue or that Elasticsearch has
-- fully caught up.

CREATE TABLE IF NOT EXISTS public.ccd_data_migration_status (
    migration_name text PRIMARY KEY,
    completed_at timestamp with time zone NOT NULL DEFAULT now()
);

DO $$
DECLARE
    marked_by_logstash_exists boolean;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'case_data'
          AND column_name = 'marked_by_logstash'
    )
    INTO marked_by_logstash_exists;

    IF marked_by_logstash_exists
        AND EXISTS (SELECT 1 FROM public.case_data LIMIT 1)
        AND NOT EXISTS (
            SELECT 1
            FROM public.ccd_data_migration_status
            WHERE migration_name = 'CCD-4790-marked-by-logstash-drop-ready'
        ) THEN
        RAISE EXCEPTION
            'CCD-4790 blocked: data migration completion marker CCD-4790-marked-by-logstash-drop-ready is missing';
    END IF;
END $$;

DROP TRIGGER IF EXISTS trg_case_data_updated ON public.case_data;

DROP FUNCTION IF EXISTS public.set_case_data_marked_by_logstash();

DROP INDEX IF EXISTS public.idx_case_data_marked_by_logstash;

ALTER TABLE public.case_data
    DROP CONSTRAINT IF EXISTS case_pointer_always_marked_by_logstash;

ALTER TABLE public.case_data
    DROP COLUMN IF EXISTS marked_by_logstash;
