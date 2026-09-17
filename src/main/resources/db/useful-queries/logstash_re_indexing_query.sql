-- Re-queue case data for Elasticsearch re-indexing without updating case_data.
--
-- Run after recreating the target Elasticsearch indexes. The loop inserts at most
-- 1000 queue rows at a time and does not duplicate work already waiting in the queue.
-- Narrow the SELECT with a jurisdiction, case type, reference list or time window
-- when recovering a known Elasticsearch failure window.
DO $$
DECLARE
    rows_queued integer;
BEGIN
    LOOP
        WITH candidates AS (
            SELECT cd.id
            FROM case_data cd
            WHERE NOT (cd.data = '{}'::jsonb AND cd.state = '')
              AND NOT EXISTS (
                  SELECT 1
                  FROM case_data_logstash_queue q
                  WHERE q.case_data_id = cd.id
              )
            ORDER BY cd.id
            LIMIT 1000
        )
        INSERT INTO case_data_logstash_queue (case_data_id)
        SELECT id FROM candidates;

        GET DIAGNOSTICS rows_queued = ROW_COUNT;
        EXIT WHEN rows_queued = 0;
    END LOOP;
END $$;
