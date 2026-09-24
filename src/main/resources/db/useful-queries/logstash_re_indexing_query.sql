-- Re-queue case data without updating case_data. Requires the unique queue
-- constraint and queue-ID version migrations.
-- Prerequisites:
-- Full rebuild after index removal: use "Create Elasticsearch Indices" in
-- ccd-admin-web to create missing case-type indexes and configure mappings.
-- Use "Create Global Search Indices" separately if rebuilding Global Search.
-- These actions do not queue case data or delete existing indexes.
-- Targeted recovery: keep existing indexes and filter this query to affected cases.
-- Old case-data cleanup is optional, not a prerequisite for re-indexing.
-- This script queues cases; Logstash consumes the queue and sends case documents
-- to its configured Elasticsearch destinations.
--
-- Set recovery_name below. Reuse it with the SAME filters to resume after interruption.
-- Use a NEW name for a fresh recovery or whenever target indexes are recreated.
-- Execute the entire DO block with autocommit enabled, outside BEGIN/COMMIT.
-- The persistent checkpoint and queue inserts commit together every 1000 cases.
-- Both roll back if a batch fails; resuming skips only previously committed batches.
-- New cases and updates may continue: normal database triggers queue them
-- independently, including updates to cases already passed by this checkpoint.
-- Those triggers must remain enabled; this pass stops at its saved maximum case ID.
-- Completed runs are retained and rerunning their name does nothing.
-- Queueing is not ES delivery: verify indexes and check output failures/DLQ.
-- Logstash deletes queue rows before ES confirms delivery. Resuming does not retry
-- failed delivery from committed batches: resolve the failure, then use a NEW
-- recovery name with filters covering the affected cases and verify ES delivery.
DO $$
DECLARE
    recovery_name TEXT := 'SET-RUN-NAME';
    batch_size INT := 1000;
    rows_queued INT;
    next_case_id BIGINT;
    progress RECORD;
BEGIN
    IF recovery_name = 'SET-RUN-NAME' THEN
        RAISE EXCEPTION 'Set recovery_name: reuse it to resume, or choose a new name for a fresh recovery';
    END IF;

    -- Serialize first-time table creation, including concurrent recovery sessions.
    PERFORM pg_advisory_xact_lock(hashtextextended('ccd_logstash_reindex_progress', 0));
    CREATE TABLE IF NOT EXISTS public.logstash_reindex_progress (
        run_name TEXT PRIMARY KEY,
        upper_case_id BIGINT,
        last_case_id BIGINT,
        total_queued BIGINT NOT NULL DEFAULT 0,
        completed BOOLEAN NOT NULL DEFAULT false
    );
    -- Bound this pass using the primary-key index; avoid counting/sorting all cases.
    INSERT INTO public.logstash_reindex_progress (run_name, upper_case_id)
    SELECT recovery_name, (SELECT max(id) FROM case_data)
    ON CONFLICT ON CONSTRAINT logstash_reindex_progress_pkey DO NOTHING;
    COMMIT;

    LOOP
        -- Same-name sessions serialize each batch and always reload saved progress.
        SELECT p.* INTO progress
        FROM public.logstash_reindex_progress p
        WHERE p.run_name = recovery_name
        FOR UPDATE;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Checkpoint for run % was removed; stop and verify recovery state.', recovery_name;
        END IF;
        IF progress.completed THEN
            RAISE NOTICE 'Run % already complete; use a new name for a fresh recovery.', recovery_name;
            COMMIT;
            EXIT;
        END IF;
        WITH batch AS (
            SELECT cd.id
            FROM case_data cd
            WHERE cd.id >= COALESCE(progress.last_case_id, '-9223372036854775808'::bigint)
              AND cd.id <= progress.upper_case_id
              AND (progress.last_case_id IS NULL OR cd.id > progress.last_case_id)
              AND NOT (cd.data = '{}'::jsonb AND cd.state = '')
              -- Add jurisdiction, case type, reference or time-window filters here.
              -- Keep filters unchanged when resuming this recovery name.
            ORDER BY cd.id
            LIMIT batch_size
            -- Prevent deletion before commit; do not skip locked cases.
            FOR KEY SHARE OF cd
        ), queued AS (
            INSERT INTO case_data_logstash_queue (case_data_id)
            SELECT id FROM batch ORDER BY id
            ON CONFLICT (case_data_id) DO NOTHING
            RETURNING case_data_id
        )
        SELECT (SELECT max(id) FROM batch), (SELECT count(*) FROM queued)
        INTO next_case_id, rows_queued;

        -- Advance over already-queued cases too. A failed batch rolls back both
        -- its queue inserts and checkpoint; previously committed batches survive.
        UPDATE public.logstash_reindex_progress p
        SET last_case_id = COALESCE(next_case_id, p.last_case_id),
            completed = next_case_id IS NULL,
            total_queued = p.total_queued + rows_queued
        WHERE p.run_name = recovery_name;
        COMMIT;
        IF next_case_id IS NULL THEN
            RAISE NOTICE 'Run % complete: queued %. Verify Elasticsearch delivery.',
                recovery_name, progress.total_queued;
            EXIT;
        END IF;
        RAISE NOTICE 'Run %: queued %, last committed case ID %',
            recovery_name, rows_queued, next_case_id;
    END LOOP;
END $$;
