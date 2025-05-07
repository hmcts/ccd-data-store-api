-- =================================================================================
-- This script deletes the records from the tables by last_modified and case_data_id criteria.
-- The select retrieves the case_data_ids to create the temp table to be used as resource of case_data_ids in delete statements.
-- Either of caseTypeReferences or jurisdictionReferences arrays can be used as per the requirement.

DO $$
DECLARE

jurisdictionReferences constant text[] := array['AUTOTEST1', 'AUTOTEST2', 'BEFTA_JURISDICTION_1', 'BEFTA_JURISDICTION_2', 'BEFTA_JURISDICTION_3', 'BEFTA_MASTER'];

caseTypeReferences constant text[] := array['AAT','AAT_AUTH_15','AAT_AUTH_2','AAT_AUTH_4','AAT_AUTH_5','AAT_AUTH_8','AAT_PRIVATE','AAT_PRIVATE_B','AllDataTypes2','CaseProgression',
                                            			'CaseViewCallbackMessages','ComplexCollectionComplex','ComplexCRUD','Conditionals','MAPPER','MultiplePages','Questions2','SC_Private',
                                            			'SC_Public','SC_Restricted','Tabs','WBSortOrder','AAT_PRIVATE2','BEFTA_CASETYPE_1_1','BEFTA_CASETYPE_2_1','BEFTA_CASETYPE_3_1','BEFTA_CASETYPE_3_2','BEFTA_CASETYPE_3_4',
                                            			'FT_ComplexCollectionComplex','FT_ConditionalPostState','FT_CRUD','FT_CRUD_2','FT_CRUD_3','FT_DateTimeFormats','FT_GlobalSearch','FT_MasterCaseType'];

BEGIN

CREATE TEMP TABLE tmp_case_data_ids ON COMMIT DROP AS
		   SELECT id
			 FROM case_data cd
			WHERE cd.last_modified <= 'now'::timestamp - '3 MONTH'::interval
			  and cd.jurisdiction = any(jurisdictionReferences)
			  --AND case_type_id = any(caseTypeReferences)
			  ;

    DELETE FROM case_users_audit WHERE case_data_id IN (
        select id from tmp_case_data_ids
    );

    DELETE FROM case_users WHERE case_data_id IN (
        select id from tmp_case_data_ids
    );

    DELETE FROM case_event_significant_items WHERE case_event_id IN (
        SELECT id FROM case_event WHERE case_data_id IN (
            select id from tmp_case_data_ids
        )
    );

    DELETE FROM case_event WHERE case_data_id IN (
        select id from tmp_case_data_ids
    );


    DELETE FROM all_events WHERE case_id IN (
        select id from tmp_case_data_ids
    );

    DELETE FROM case_data WHERE id in (select id from tmp_case_data_ids);

END $$;


-- New Logic
-- Start
-- Step 1: Create the temp table
DROP TABLE IF EXISTS tmp_case_data_ids;

CREATE TEMP TABLE tmp_case_data_ids AS
SELECT id
FROM case_data
WHERE last_modified <= now() - INTERVAL '3 MONTH'
ORDER BY id ASC LIMIT 50000;

-- Step 2: Looped deletes for each dependent table

-- case_users_audit
DO $$
DECLARE r INT;
BEGIN
  LOOP
    DELETE FROM case_users_audit
    WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids LIMIT 10000);
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
    WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids LIMIT 10000);
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
      WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids LIMIT 10000)
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
    WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids LIMIT 10000);
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
    WHERE case_id IN (SELECT id FROM tmp_case_data_ids LIMIT 10000);
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
    WHERE case_id IN (SELECT id FROM tmp_case_data_ids LIMIT 10000);
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
    WHERE linked_case_id IN (SELECT id FROM tmp_case_data_ids LIMIT 10000);
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
    WHERE id IN (SELECT id FROM tmp_case_data_ids LIMIT 10000);
    GET DIAGNOSTICS r = ROW_COUNT;
    RAISE NOTICE 'case_data, Deleted % rows.', r;
    EXIT WHEN r = 0;
    PERFORM pg_sleep(0.1);
  END LOOP;
END$$;

-- End
