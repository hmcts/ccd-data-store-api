-- -Assumptions:
-- 1. General data-store cleanup performed (removing all case_types older than X months)
-- 1. Deletion of all ES indexes performed (curl -XDELETE <ES node IP address>:9200/_all;)
--     (ES node IP address details can be found here: 
--     https://tools.hmcts.net/confluence/display/RCCD/Connecting+to+and+deleting+data+from+CCD+Data+Store+and+CCD+Definition+Store)
-- 2. ES re-indexing triggered via ccd-admin-web 
--     (note, this only creates the static indexes i.e place holders)

-- 3. Run the below script after the target Elasticsearch indexes have been removed.
--    It repopulates the queue in batches so Logstash can re-index all cases.
-- 4. Queue rows are leased by Logstash and become eligible for retry when their lease expires.
--    The preview deployment defaults LOGSTASH_QUEUE_CLAIM_TIMEOUT to 5 minutes; adjust it through
--    the Logstash deployment configuration if normal indexing can take longer.

-- Run once per re-index. Re-running creates additional queue attempts by design.
INSERT INTO case_data_logstash_queue (case_data_id)
SELECT id
FROM case_data
WHERE NOT (data = '{}'::jsonb AND state = '');

-- Expired claims are automatically eligible for retry by the Logstash claim query.
SELECT COUNT(*) AS queued_for_reindex
FROM case_data_logstash_queue
WHERE claimed_at IS NULL;
