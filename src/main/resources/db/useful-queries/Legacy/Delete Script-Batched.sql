-- Start
-- Step 1: Create the temp table
DROP TABLE IF EXISTS tmp_case_data_ids;

CREATE TEMP TABLE tmp_case_data_ids AS
SELECT id
FROM case_data
WHERE last_modified <= now() - INTERVAL '3 MONTH'
ORDER BY id ASC;

-- Step 2: Looped deletes for each dependent table

DO
$$
DECLARE
    batch_size CONSTANT INTEGER := 1000;
    rows_deleted INTEGER;
BEGIN
    -- Create log output table if not exists
    CREATE TABLE IF NOT EXISTS ddl_log (
        log_time TIMESTAMP DEFAULT now(),
        action TEXT,
        table_name TEXT,
        message TEXT
    );

    PERFORM pg_sleep(1);

    PERFORM pg_sleep(1);

    -- - Delete from case_users_audit
    BEGIN
        <<batch_delete_loop_case_users_audit>>
        LOOP
            WITH to_delete AS (
                SELECT case_data_id FROM case_users_audit
                WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids)
                LIMIT batch_size
            ),
            del AS (
                DELETE FROM case_users_audit
                WHERE case_data_id IN (SELECT case_data_id FROM to_delete)
                RETURNING *
            )
            SELECT COUNT(*) INTO rows_deleted FROM del;

            EXIT batch_delete_loop_case_users_audit WHEN rows_deleted = 0;
            RAISE NOTICE 'Deleted % rows from case_users_audit', rows_deleted;
            INSERT INTO ddl_log(action, table_name, message)
            VALUES ('DELETE', 'case_users_audit', 'Deleted ' || rows_deleted || ' rows from case_users_audit');
        END LOOP batch_delete_loop_case_users_audit;
        PERFORM pg_sleep(1);
    EXCEPTION
        WHEN OTHERS THEN
            RAISE NOTICE 'Skipping deletion from case_users_audit due to error: %', SQLERRM;
    END;

    -- - Delete from case_users
    BEGIN
        <<batch_delete_loop_case_users>>
        LOOP
            WITH to_delete AS (
                SELECT case_data_id FROM case_users
                WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids)
                LIMIT batch_size
            ),
            del AS (
                DELETE FROM case_users
                WHERE case_data_id IN (SELECT case_data_id FROM to_delete)
                RETURNING *
            )
            SELECT COUNT(*) INTO rows_deleted FROM del;

            EXIT batch_delete_loop_case_users WHEN rows_deleted = 0;
            RAISE NOTICE 'Deleted % rows from case_users', rows_deleted;
            INSERT INTO ddl_log(action, table_name, message)
            VALUES ('DELETE', 'case_users', 'Deleted ' || rows_deleted || ' rows from case_users');
        END LOOP batch_delete_loop_case_users;
        PERFORM pg_sleep(1);
    EXCEPTION
        WHEN OTHERS THEN
            RAISE NOTICE 'Skipping deletion from case_users due to error: %', SQLERRM;
    END;

    -- - Delete from case_event_significant_items
    BEGIN
        <<batch_delete_loop_case_event_significant_items>>
        LOOP
            WITH to_delete AS (
                SELECT id FROM case_event
                WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids)
                LIMIT batch_size
            ),
            del AS (
                DELETE FROM case_event_significant_items
               WHERE case_event_id IN (SELECT id FROM to_delete)
                RETURNING *
            )
            SELECT COUNT(*) INTO rows_deleted FROM del;

            EXIT batch_delete_loop_case_event_significant_items WHEN rows_deleted = 0;
            RAISE NOTICE 'Deleted % rows from case_event_significant_items', rows_deleted;
            INSERT INTO ddl_log(action, table_name, message)
            VALUES ('DELETE', 'case_event_significant_items', 'Deleted ' || rows_deleted || ' rows from case_event_significant_items');
        END LOOP batch_delete_loop_case_event_significant_items;
        PERFORM pg_sleep(1);
    EXCEPTION
        WHEN OTHERS THEN
            RAISE NOTICE 'Skipping deletion from case_event_significant_items due to error: %', SQLERRM;
    END;    

    -- - Delete from case_event
    BEGIN
        <<batch_delete_loop_case_event>>
        LOOP
            WITH to_delete AS (
                SELECT case_data_id FROM case_event
                WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids)
                LIMIT batch_size
            ),
            del AS (
                DELETE FROM case_event
                WHERE case_data_id IN (SELECT case_data_id FROM to_delete)
                RETURNING *
            )
            SELECT COUNT(*) INTO rows_deleted FROM del;

            EXIT batch_delete_loop_case_event WHEN rows_deleted = 0;
            RAISE NOTICE 'Deleted % rows from case_event', rows_deleted;
            INSERT INTO ddl_log(action, table_name, message)
            VALUES ('DELETE', 'case_event', 'Deleted ' || rows_deleted || ' rows from case_event');
        END LOOP batch_delete_loop_case_event;
        PERFORM pg_sleep(1);
    EXCEPTION
        WHEN OTHERS THEN
            RAISE NOTICE 'Skipping deletion from case_event due to error: %', SQLERRM;
    END; 

    -- - Delete from all_events
    BEGIN
        <<batch_delete_loop_case_event_2>>
        LOOP
            WITH to_delete AS (
                SELECT case_id FROM all_events
                WHERE case_id IN (SELECT id FROM tmp_case_data_ids)
                LIMIT batch_size
            ),
            del AS (
                DELETE FROM all_events
                WHERE case_id IN (SELECT case_data_id FROM to_delete)
                RETURNING *
            )
            SELECT COUNT(*) INTO rows_deleted FROM del;

            EXIT batch_delete_loop_case_event_2 WHEN rows_deleted = 0;
            RAISE NOTICE 'Deleted % rows from all_events', rows_deleted;
            INSERT INTO ddl_log(action, table_name, message)
            VALUES ('DELETE', 'all_events', 'Deleted ' || rows_deleted || ' rows from all_events');
        END LOOP batch_delete_loop_case_event_2;
        PERFORM pg_sleep(1);
    EXCEPTION
        WHEN OTHERS THEN
            RAISE NOTICE 'Skipping deletion from all_events due to error: %', SQLERRM;
    END; 

    -- - Delete from case_link
    BEGIN
        <<batch_delete_loop_case_link>>
        LOOP
            WITH to_delete AS (
                SELECT case_id FROM case_link
                WHERE case_id IN (SELECT id FROM tmp_case_data_ids)
                LIMIT batch_size
            ),
            del AS (
                DELETE FROM case_link
                WHERE case_id IN (SELECT case_data_id FROM to_delete)
                RETURNING *
            )
            SELECT COUNT(*) INTO rows_deleted FROM del;

            EXIT batch_delete_loop_case_link WHEN rows_deleted = 0;
            RAISE NOTICE 'Deleted % rows from case_link', rows_deleted;
            INSERT INTO ddl_log(action, table_name, message)
            VALUES ('DELETE', 'case_link', 'Deleted ' || rows_deleted || ' rows from case_link');
        END LOOP batch_delete_loop_case_link;
        PERFORM pg_sleep(1);
    EXCEPTION
        WHEN OTHERS THEN
            RAISE NOTICE 'Skipping deletion from case_link due to error: %', SQLERRM;
    END; 

    -- - Delete from case_link (linked_case_id)
    BEGIN
        <<batch_delete_loop_case_link_2>>
        LOOP
            WITH to_delete AS (
                SELECT linked_case_id FROM case_link
                WHERE linked_case_id IN (SELECT id FROM tmp_case_data_ids)
                LIMIT batch_size
            ),
            del AS (
                DELETE FROM case_link
                WHERE linked_case_id IN (SELECT case_data_id FROM to_delete)
                RETURNING *
            )
            SELECT COUNT(*) INTO rows_deleted FROM del;

            EXIT batch_delete_loop_case_link_2 WHEN rows_deleted = 0;
            RAISE NOTICE 'Deleted % rows from case_link', rows_deleted;
            INSERT INTO ddl_log(action, table_name, message)
            VALUES ('DELETE', 'case_link', 'Deleted ' || rows_deleted || ' rows from case_link');
        END LOOP batch_delete_loop_case_link_2;
        PERFORM pg_sleep(1);
    EXCEPTION
        WHEN OTHERS THEN
            RAISE NOTICE 'Skipping deletion from case_link due to error: %', SQLERRM;
    END; 

    -- - Delete from case_data (final cleanup)
    BEGIN
        <<batch_delete_loop_case_data>>
        LOOP
            WITH to_delete AS (
                SELECT id FROM case_data
                WHERE id IN (SELECT id FROM tmp_case_data_ids)
                LIMIT batch_size
            ),
            del AS (
                DELETE FROM case_data
                WHERE id IN (SELECT case_data_id FROM to_delete)
                RETURNING *
            )
            SELECT COUNT(*) INTO rows_deleted FROM del;

            EXIT batch_delete_loop_case_data WHEN rows_deleted = 0;
            RAISE NOTICE 'Deleted % rows from case_data', rows_deleted;
            INSERT INTO ddl_log(action, table_name, message)
            VALUES ('DELETE', 'case_data', 'Deleted ' || rows_deleted || ' rows from case_data');
        END LOOP batch_delete_loop_case_data;
        PERFORM pg_sleep(1);
    EXCEPTION
        WHEN OTHERS THEN
            RAISE NOTICE 'Skipping deletion from case_data due to error: %', SQLERRM;
    END;
END
$$;
