-- CCD-8236: replay one controlled batch of existing Global Search cases so
-- data.nextHearingDetails is copied into the global_search index.
--
-- Run this script only after the Global Search mapping and Logstash pipeline
-- changes have been deployed. Repeat it in the same database session while
-- monitoring Logstash until it returns 0. Each execution marks at most 1,000
-- eligible cases for replay.

CREATE TEMPORARY TABLE IF NOT EXISTS ccd_8236_replay_queue (
    case_data_id BIGINT PRIMARY KEY
) ON COMMIT PRESERVE ROWS;

CREATE TEMPORARY TABLE IF NOT EXISTS ccd_8236_replay_state (
    initialised BOOLEAN PRIMARY KEY
) ON COMMIT PRESERVE ROWS;

INSERT INTO ccd_8236_replay_queue (case_data_id)
SELECT id
FROM case_data
WHERE marked_by_logstash = true
  AND data ? 'SearchCriteria'
  AND NULLIF(data #>> '{nextHearingDetails,hearingDateTime}', '') IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ccd_8236_replay_state);

INSERT INTO ccd_8236_replay_state (initialised)
VALUES (true)
ON CONFLICT DO NOTHING;

WITH cases_to_replay AS (
    SELECT case_data_id
    FROM ccd_8236_replay_queue
    ORDER BY case_data_id
    LIMIT 1000
    FOR UPDATE SKIP LOCKED
), marked_for_replay AS (
    UPDATE case_data
    SET marked_by_logstash = false
    WHERE id IN (SELECT case_data_id FROM cases_to_replay)
    RETURNING id
), removed_from_queue AS (
    DELETE FROM ccd_8236_replay_queue
    WHERE case_data_id IN (SELECT id FROM marked_for_replay)
    RETURNING case_data_id
)
SELECT COUNT(*) AS cases_marked_for_logstash_replay
FROM removed_from_queue;
