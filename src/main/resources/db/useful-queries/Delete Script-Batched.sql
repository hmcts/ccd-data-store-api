-- Start
-- Step 1: Create the temp table
DROP TABLE IF EXISTS tmp_case_data_ids;

CREATE TEMP TABLE tmp_case_data_ids AS
SELECT id
FROM case_data
WHERE last_modified <= now() - INTERVAL '3 MONTH'
ORDER BY id ASC;

-- Step 2: Looped deletes for each dependent table

-- case_users_audit
DO $$
DECLARE r INT;
BEGIN
  LOOP
    DELETE FROM case_users_audit
    WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids ORDER BY id ASC LIMIT 10000);
    GET DIAGNOSTICS r = ROW_COUNT;
    RAISE NOTICE 'case_users_audit, Deleted % rows.', r;
    EXIT WHEN r = 0;
    PERFORM pg_sleep(0.1);
  END LOOP;
END$$;

-- case_users
DO $$
DECLARE r INT;
BEGIN
  LOOP
    DELETE FROM case_users
    WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids ORDER BY id ASC LIMIT 10000);
    GET DIAGNOSTICS r = ROW_COUNT;
    RAISE NOTICE 'case_users, Deleted % rows.', r;
    EXIT WHEN r = 0;
    PERFORM pg_sleep(0.1);
  END LOOP;
END$$;

-- case_event_significant_items (via case_event)
DO $$
DECLARE r INT;
BEGIN
  LOOP
    DELETE FROM case_event_significant_items
    WHERE case_event_id IN (
      SELECT id FROM case_event
      WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids ORDER BY id ASC LIMIT 10000)
    );
    GET DIAGNOSTICS r = ROW_COUNT;
    RAISE NOTICE 'case_event_significant_items, Deleted % rows.', r;
    EXIT WHEN r = 0;
    PERFORM pg_sleep(0.1);
  END LOOP;
END$$;

-- case_event
DO $$
DECLARE r INT;
BEGIN
  LOOP
    DELETE FROM case_event
    WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids ORDER BY id ASC LIMIT 10000);
    GET DIAGNOSTICS r = ROW_COUNT;
    RAISE NOTICE 'case_event, Deleted % rows.', r;
    EXIT WHEN r = 0;
    PERFORM pg_sleep(0.1);
  END LOOP;
END$$;

-- all_events
DO $$
DECLARE r INT;
BEGIN
  LOOP
    DELETE FROM all_events
    WHERE case_id IN (SELECT id FROM tmp_case_data_ids ORDER BY id ASC LIMIT 10000);
    GET DIAGNOSTICS r = ROW_COUNT;
    RAISE NOTICE 'all_events, Deleted % rows.', r;
    EXIT WHEN r = 0;
    PERFORM pg_sleep(0.1);
  END LOOP;
END$$;

DO $$
DECLARE r INT;
BEGIN
  LOOP
    DELETE FROM case_link
    WHERE case_id IN (SELECT id FROM tmp_case_data_ids ORDER BY id ASC LIMIT 10000);
    GET DIAGNOSTICS r = ROW_COUNT;
    RAISE NOTICE 'case_link, Deleted % rows.', r;
    EXIT WHEN r = 0;
    PERFORM pg_sleep(0.1);
  END LOOP;
END$$;

-- case_link (linked_case_id)
DO $$
DECLARE r INT;
BEGIN
  LOOP
    DELETE FROM case_link
    WHERE linked_case_id IN (SELECT id FROM tmp_case_data_ids ORDER BY id ASC LIMIT 10000);
    GET DIAGNOSTICS r = ROW_COUNT;
    RAISE NOTICE 'case_link, Deleted % rows.', r;
    EXIT WHEN r = 0;
    PERFORM pg_sleep(0.1);
  END LOOP;
END$$;

-- case_data (final cleanup)
DO $$
DECLARE r INT;
BEGIN
  LOOP
    DELETE FROM case_data
    WHERE id IN (SELECT id FROM tmp_case_data_ids ORDER BY id ASC LIMIT 10000);
    GET DIAGNOSTICS r = ROW_COUNT;
    RAISE NOTICE 'case_data, Deleted % rows.', r;
    EXIT WHEN r = 0;
    PERFORM pg_sleep(0.1);
  END LOOP;
END$$;

-- Clean up temp table
DELETE FROM tmp_case_data_ids WHERE id IN (SELECT id FROM tmp_case_data_ids ORDER BY id ASC LIMIT 10000);

-- End