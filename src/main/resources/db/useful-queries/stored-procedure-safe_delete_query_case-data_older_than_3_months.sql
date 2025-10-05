-- -Assumptions:
-- 1. General data-store cleanup performed (removing all case_types older than X months)
-- 1. Deletion of all ES indexes performed (curl -XDELETE <ES node IP address>:9200/_all;)
--     (ES node IP address details can be found here: 
--     https://tools.hmcts.net/confluence/display/RCCD/Connecting+to+and+deleting+data+from+CCD+Data+Store+and+CCD+Definition+Store)
-- 2. ES re-indexing triggered via ccd-admin-web 
--     (note, this only creates the static indexes i.e place holders)

-- 3. Run the below script. This will loop through each Jurisdiction starting with the most cases 
--    and update the marked_by_logstash field to false in batches of 1000
-- 4. Once a row is markes as false, an automatic job is triggered perform logstash indexing

DO $$
DECLARE
    batch_size INT := 1000;
    rows_updated INT;
    total_updated INT;
    current_jurisdiction TEXT;
    start_time TIMESTAMP;
    end_time TIMESTAMP;
BEGIN
    RAISE NOTICE 'Starting batch update...';
   
    DROP TABLE IF EXISTS JurisdictionsToIndex;

    -- Create a temp table with jurisdictions sorted by count descending
    CREATE TEMP TABLE JurisdictionsToIndex AS
        SELECT jurisdiction
        FROM (
            SELECT jurisdiction, COUNT(*) AS count
            FROM case_data
            GROUP BY jurisdiction
            ORDER BY count DESC
        ) sub;

    -- Loop through each jurisdiction
    FOR current_jurisdiction IN
        SELECT jurisdiction FROM JurisdictionsToIndex
    LOOP
        total_updated := 0;
        start_time := clock_timestamp();
        RAISE NOTICE 'Processing jurisdiction: %', current_jurisdiction;

        LOOP
            -- Batch update for current jurisdiction
            WITH batch AS (
                SELECT id
                FROM case_data
                WHERE marked_by_logstash = true
                  AND jurisdiction = current_jurisdiction
                LIMIT batch_size
                FOR UPDATE SKIP LOCKED
            )
            UPDATE case_data
            SET marked_by_logstash = false
            WHERE id IN (SELECT id FROM batch);

            -- Correctly get number of rows updated
            GET DIAGNOSTICS rows_updated = ROW_COUNT;

            EXIT WHEN rows_updated = 0;
            total_updated := total_updated + rows_updated;
        END LOOP;

        end_time := clock_timestamp();
        RAISE NOTICE 'Jurisdiction %: updated %, Time taken: % seconds',
                     current_jurisdiction, total_updated, end_time - start_time;
    END LOOP;

    RAISE NOTICE 'Batch update complete.';
END $$;
