CREATE OR REPLACE PROCEDURE public.cleanup_case_data(
    IN batch_size integer DEFAULT 1000,
    IN older_than_months integer DEFAULT 3
)
LANGUAGE plpgsql
AS $procedure$
BEGIN
    ----------------------------------------------------------------------
    -- 1) CREATE HELPER FUNCTIONS (no transaction control inside)
    ----------------------------------------------------------------------

    -- Batch delete function
    EXECUTE $fn$
    CREATE OR REPLACE FUNCTION safe_delete_where(
        tbl          regclass,
        pk_column    text,
        where_clause text,
        batch_size   int DEFAULT 1000
    )
    RETURNS int
    LANGUAGE plpgsql
    AS $body$
    DECLARE
        rows_deleted int;
        total_deleted int := 0;
        full_tbl_name text := tbl::text;
    BEGIN
        <<batch_loop>>
        LOOP
            EXECUTE format(
                'WITH keys AS (
                     SELECT ctid, %I AS pk_val
                     FROM %s
                     WHERE %s
                     LIMIT %s
                 )
                 DELETE FROM %s t
                 USING keys k
                 WHERE t.ctid = k.ctid',
                pk_column, full_tbl_name, where_clause, batch_size,
                full_tbl_name
            );

            GET DIAGNOSTICS rows_deleted = ROW_COUNT;
            EXIT batch_loop WHEN rows_deleted = 0;

            total_deleted := total_deleted + rows_deleted;
            RAISE NOTICE 'Batch deleted % rows from % (running total: %)',
                rows_deleted, full_tbl_name, total_deleted;
        END LOOP;

        IF total_deleted > 0 THEN
            RAISE NOTICE 'Total deleted from %: % rows', full_tbl_name, total_deleted;
        END IF;
        
        RETURN total_deleted;
    END;
    $body$;
    $fn$;

    ----------------------------------------------------------------------
    -- Prepare temp table
    ----------------------------------------------------------------------
    EXECUTE $fn$
    CREATE OR REPLACE FUNCTION prepare_cleanup_temp_tables(older_than_months int)
    RETURNS void
    LANGUAGE plpgsql
    AS $body$
    DECLARE
        row_count int;
    BEGIN
        BEGIN
            DROP TABLE IF EXISTS case_ids_to_remove;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Error dropping case_ids_to_remove: %', SQLERRM;
        END;

        CREATE TABLE IF NOT EXISTS ddl_log (
            log_time TIMESTAMP DEFAULT now(),
            action TEXT,
            table_name TEXT,
            message TEXT
        );

        EXECUTE format(
            'CREATE TEMP TABLE case_ids_to_remove AS
             SELECT id
             FROM case_data
             WHERE last_modified <= now() - INTERVAL ''%s MONTH''
             ORDER BY id ASC;',
            older_than_months
        );

        EXECUTE 'SELECT COUNT(*) FROM case_ids_to_remove' INTO row_count;

        RAISE NOTICE 'Created temp table case_ids_to_remove for records older than % months with % rows',
            older_than_months, row_count;

        INSERT INTO ddl_log(action, table_name, message)
        VALUES ('PREPARE', 'case_ids_to_remove',
                format('older_than_months=%s; rows=%s', older_than_months, row_count));
    END;
    $body$;
    $fn$;

    ----------------------------------------------------------------------
    -- Drop temp table
    ----------------------------------------------------------------------
    EXECUTE $fn$
    CREATE OR REPLACE FUNCTION drop_cleanup_temp_tables()
    RETURNS void
    LANGUAGE plpgsql
    AS $body$
    BEGIN
        BEGIN
            DROP TABLE IF EXISTS case_ids_to_remove;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Error dropping case_ids_to_remove: %', SQLERRM;
        END;
    END;
    $body$;
    $fn$;
    
    CREATE OR REPLACE PROCEDURE delete_table_with_periodic_commit(
    tbl          regclass,
    pk_column    text,
    where_clause text,
    batch_size   int DEFAULT 1000,
    commit_every int DEFAULT 5000   -- 👈 commit every N rows
    )
    LANGUAGE plpgsql
    AS $$
    DECLARE
        deleted_this_batch int;
        total_deleted int := 0;
        full_tbl_name text := tbl::text;
    BEGIN
        RAISE NOTICE 'Starting batched delete on table % (commit every % rows)...', full_tbl_name, commit_every;
    
        LOOP
            -- run one batch
            deleted_this_batch := safe_delete_where(tbl, pk_column, where_clause, batch_size);
            EXIT WHEN deleted_this_batch = 0;
    
            total_deleted := total_deleted + deleted_this_batch;
    
            -- commit after every commit_every rows
            IF total_deleted % commit_every = 0 THEN
                RAISE NOTICE 'Committing after % total deletes on %', total_deleted, full_tbl_name;
                COMMIT;
                START TRANSACTION;
            END IF;
        END LOOP;
    
        COMMIT;
        RAISE NOTICE 'Completed deletes on %: total % rows deleted', full_tbl_name, total_deleted;
    END;
    $$;

    ----------------------------------------------------------------------
    -- Delete procedure for one table (no commits here)
    ----------------------------------------------------------------------
    EXECUTE $fn$
    CREATE OR REPLACE PROCEDURE delete_one_table(
        tbl          regclass,
        pk_column    text,
        where_clause text,
        batch_size   int DEFAULT 1000
    )
    LANGUAGE plpgsql
    AS $body$
    DECLARE
        deleted_this_round int;
        total_deleted int := 0;
        full_tbl_name text := tbl::text;
    BEGIN
        LOOP
            deleted_this_round := safe_delete_where(tbl, pk_column, where_clause, batch_size);
            EXIT WHEN deleted_this_round = 0;
            total_deleted := total_deleted + deleted_this_round;
        END LOOP;

        RAISE NOTICE 'Finished table %: total deleted % rows', full_tbl_name, total_deleted;
        INSERT INTO ddl_log(action, table_name, message)
        VALUES ('DELETE SUMMARY', full_tbl_name,
                format('Total deleted %s rows', total_deleted));
    END;
    $body$;
    $fn$;

    ----------------------------------------------------------------------
    -- 2) RUN PIPELINE (Postgres auto-commits after CALL finishes)
    ----------------------------------------------------------------------
    RAISE NOTICE 'Step 1: Preparing temp tables (older_than_months=%)...', older_than_months;
    PERFORM prepare_cleanup_temp_tables(older_than_months);

    IF NOT EXISTS (SELECT 1 FROM case_ids_to_remove) THEN
        RAISE NOTICE 'No rows to delete in case_ids_to_remove. Proceeding to cleanup.';
    ELSE
        RAISE NOTICE 'Step 2: Deleting in dependency order (children → parent)...';

        -- 2.1 case_event_significant_items
        CALL delete_one_table(
            'case_event_significant_items',
            'case_event_id',
            'case_event_id IN (
               SELECT id FROM case_event
               WHERE case_data_id IN (SELECT id FROM case_ids_to_remove)
             )',
            batch_size
        );

        -- 2.2 case_event
        CALL delete_one_table(
            'case_event',
            'case_data_id',
            'case_data_id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );

        -- 2.3 case_users_audit
        CALL delete_one_table(
            'case_users_audit',
            'case_data_id',
            'case_data_id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );

        -- 2.4 case_users
        CALL delete_one_table(
            'case_users',
            'case_data_id',
            'case_data_id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );

        -- 2.5 all_events
        CALL delete_one_table(
            'all_events',
            'case_id',
            'case_id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );

        -- 2.6 case_link by case_id
        CALL delete_one_table(
            'case_link',
            'case_id',
            'case_id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );

        -- 2.7 case_link by linked_case_id
        CALL delete_one_table(
            'case_link',
            'linked_case_id',
            'linked_case_id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );

        -- 2.8 case_data (parent last)
        CALL delete_table_with_periodic_commit(
            'case_data',
            'id',
            'id IN (SELECT id FROM case_ids_to_remove)',
            batch_size,
            5000  -- 👈 commit every 5000 records
        );
    END IF;

    RAISE NOTICE 'Step 3: Dropping temp tables...';
    PERFORM drop_cleanup_temp_tables();

    ----------------------------------------------------------------------
    -- 3) CLEANUP HELPERS (drop temporary routines)
    ----------------------------------------------------------------------
    EXECUTE 'DROP FUNCTION IF EXISTS safe_delete_where(regclass, text, text, int4)';
    EXECUTE 'DROP FUNCTION IF EXISTS prepare_cleanup_temp_tables(int4)';
    EXECUTE 'DROP FUNCTION IF EXISTS drop_cleanup_temp_tables()';
    EXECUTE 'DROP PROCEDURE IF EXISTS delete_one_table(regclass, text, text, int4)';
    EXECUTE 'DROP PROCEDURE IF EXISTS delete_table_with_periodic_commit(regclass, text, text, int4, int4)';

    RAISE NOTICE
        'cleanup_case_data finished successfully for data older than % months (batch_size=%)',
        older_than_months, batch_size;
END;
$procedure$;
