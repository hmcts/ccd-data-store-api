-- Assumptions:
-- 1. General data-store cleanup performed (removing all case_types older than X months)
-- 2. Deletion of all ES indexes performed (curl -XDELETE <ES node IP address>:9200/_all;)
--    ES node IP address details can be found here:
--    https://tools.hmcts.net/confluence/display/RCCD/Connecting+to+and+deleting+data+from+CCD+Data+Store+and+CCD+Definition+Store
-- 3. ES re-indexing triggered via ccd-admin-web
--    This only creates the static index placeholders.
-- 4. Run the below script. This queues non-pointer case_data rows in case_data_logstash_queue
--    by jurisdiction, in batches of 1000.
-- 5. Logstash will consume and delete queued rows as part of the normal indexing pipeline.

DO $$
DECLARE
    batch_size INT := 1000;
    rows_queued INT;
    total_queued INT;
    current_jurisdiction TEXT;
    start_time TIMESTAMP;
    end_time TIMESTAMP;
BEGIN
    RAISE NOTICE 'Starting batch queueing...';

    DROP TABLE IF EXISTS CaseDataToIndex;

    CREATE TEMP TABLE CaseDataToIndex ON COMMIT DROP AS
        SELECT id, jurisdiction
        FROM case_data
        WHERE NOT (data = '{}'::jsonb AND state = '');

    CREATE INDEX case_data_to_index_jurisdiction_id_idx
        ON CaseDataToIndex (jurisdiction, id);

    DROP TABLE IF EXISTS JurisdictionsToIndex;

    CREATE TEMP TABLE JurisdictionsToIndex ON COMMIT DROP AS
        SELECT jurisdiction
        FROM CaseDataToIndex
        GROUP BY jurisdiction
        ORDER BY COUNT(*) DESC;

    FOR current_jurisdiction IN
        SELECT jurisdiction FROM JurisdictionsToIndex
    LOOP
        total_queued := 0;
        start_time := clock_timestamp();
        RAISE NOTICE 'Processing jurisdiction: %', current_jurisdiction;

        LOOP
            WITH batch AS (
                SELECT id
                FROM CaseDataToIndex
                WHERE jurisdiction = current_jurisdiction
                ORDER BY id
                LIMIT batch_size
                FOR UPDATE SKIP LOCKED
            ),
            queued AS (
                INSERT INTO case_data_logstash_queue (case_data_id)
                SELECT id FROM batch
                RETURNING case_data_id
            )
            DELETE FROM CaseDataToIndex pending
            USING queued
            WHERE pending.id = queued.case_data_id;

            GET DIAGNOSTICS rows_queued = ROW_COUNT;

            EXIT WHEN rows_queued = 0;
            total_queued := total_queued + rows_queued;
        END LOOP;

        end_time := clock_timestamp();
        RAISE NOTICE 'Jurisdiction %: queued %, Time taken: % seconds',
                     current_jurisdiction, total_queued, end_time - start_time;
    END LOOP;

    RAISE NOTICE 'Batch queueing complete.';
END $$;
