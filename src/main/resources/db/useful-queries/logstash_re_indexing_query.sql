-- Re-queue case data for Elasticsearch re-indexing without updating case_data.
-- Run after recreating the target Elasticsearch indexes via ccd-admin-web.
-- Requires the unique queue constraint and queue-ID version migrations.
-- Execute this entire DO block with autocommit enabled, outside BEGIN/COMMIT:
-- each batch commits separately so Logstash can drain the queue during the run.
-- Completed batches survive an interruption; rerunning starts a fresh pass.
-- Queueing completion does not confirm ES delivery: check output failures/DLQ
-- and verify the target indexes; re-queue affected cases after resolving failures.
DO $$
DECLARE
    batch_size INT := 1000;
    rows_queued INT;
    total_queued BIGINT;
    current_jurisdiction TEXT;
    last_case_id BIGINT;
    next_case_id BIGINT;
    upper_case_id BIGINT;
    start_time TIMESTAMP;
BEGIN
    -- Bound this pass. New cases and concurrent updates use the normal trigger.
    SELECT max(id) INTO upper_case_id FROM case_data;

    RAISE NOTICE 'Starting re-queue through case ID %...', upper_case_id;
    FOR current_jurisdiction IN
        SELECT jurisdiction
        FROM case_data
        WHERE id <= upper_case_id
        GROUP BY jurisdiction
        ORDER BY count(*) DESC, jurisdiction
    LOOP
        last_case_id := NULL;
        total_queued := 0;
        start_time := clock_timestamp();
        RAISE NOTICE 'Processing jurisdiction: %', current_jurisdiction;

        LOOP
            WITH batch AS (
                SELECT cd.id
                FROM case_data cd
                WHERE cd.jurisdiction IS NOT DISTINCT FROM current_jurisdiction
                  AND cd.id <= upper_case_id
                  AND (last_case_id IS NULL OR cd.id > last_case_id)
                  AND NOT (cd.data = '{}'::jsonb AND cd.state = '')
                  -- Add case type, reference or time-window filters here for targeted recovery.
                ORDER BY cd.id
                LIMIT batch_size
                -- Prevent deletion before the queue insert commits. Do not skip locked cases.
                FOR KEY SHARE OF cd
            ), queued AS (
                INSERT INTO case_data_logstash_queue (case_data_id)
                SELECT id FROM batch ORDER BY id
                ON CONFLICT (case_data_id) DO NOTHING
                RETURNING case_data_id
            )
            SELECT (SELECT max(id) FROM batch), (SELECT count(*) FROM queued)
            INTO next_case_id, rows_queued;

            COMMIT;
            EXIT WHEN next_case_id IS NULL;
            -- Advance even if every candidate was already queued, or Logstash drained them.
            last_case_id := next_case_id;
            total_queued := total_queued + rows_queued;
        END LOOP;

        RAISE NOTICE 'Jurisdiction %: queued %, Time taken: %',
            current_jurisdiction, total_queued, clock_timestamp() - start_time;
    END LOOP;
    RAISE NOTICE 'Re-queue pass complete; verify Elasticsearch delivery.';
END $$;
