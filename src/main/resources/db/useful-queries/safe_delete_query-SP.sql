CREATE OR REPLACE PROCEDURE cleanup_case_data(batch_size int DEFAULT 1000)
LANGUAGE plpgsql
AS $$
BEGIN
    ----------------------------------------------------------------------
    -- 1. CREATE ALL REQUIRED FUNCTIONS
    ----------------------------------------------------------------------

    -- safe_delete_query
    EXECUTE $fn$
    CREATE OR REPLACE FUNCTION safe_delete_query(
        delete_sql   text,
        tbl          regclass,
        pk_column    text,
        batch_size   int DEFAULT 1000
    )
    RETURNS void
    LANGUAGE plpgsql
    AS $body$
    DECLARE
        rows_deleted int;
        total_deleted int := 0;
        full_tbl_name text := tbl::text;
        base_table text;
        alias_name text;
        select_pk_sql text;
        m text[];
    BEGIN
        SELECT regexp_matches(
                 delete_sql,
                 '^DELETE\s+FROM\s+("?[\w]+"?)(?:\s+([A-Za-z_][A-Za-z0-9_]*))?(.*)$',
                 'i'
               )
          INTO m;

        IF m IS NULL THEN
            RAISE EXCEPTION 'Unsupported DELETE form: %', delete_sql;
        END IF;

        base_table := m[1];
        alias_name := m[2];

        IF alias_name IS NOT NULL THEN
            select_pk_sql := format(
                'SELECT DISTINCT %s.%I AS pk_val, %s.ctid
                 FROM %s %s %s',
                alias_name, pk_column, alias_name,
                base_table, alias_name, coalesce(m[3],'')
            );
        ELSE
            select_pk_sql := format(
                'SELECT DISTINCT %I AS pk_val, %s.ctid
                 FROM %s %s',
                pk_column, base_table,
                base_table, coalesce(m[3],'')
            );
        END IF;

        LOOP
            EXECUTE format(
                'WITH keys AS (%s LIMIT %s)
                 DELETE FROM %s t
                 USING keys k
                 WHERE t.ctid = k.ctid',
                select_pk_sql,
                batch_size,
                full_tbl_name
            );

            GET DIAGNOSTICS rows_deleted = ROW_COUNT;
            EXIT WHEN rows_deleted = 0;
            total_deleted := total_deleted + rows_deleted;

            RAISE NOTICE 'Batch deleted % rows from %', rows_deleted, full_tbl_name;
        END LOOP;

        RAISE NOTICE 'Total deleted from %: % rows', full_tbl_name, total_deleted;
    END;
    $body$;
    $fn$;

    ----------------------------------------------------------------------
    -- safe_delete_where
    ----------------------------------------------------------------------
    EXECUTE $fn$
    CREATE OR REPLACE FUNCTION safe_delete_where(
        tbl          regclass,
        pk_column    text,
        where_clause text,
        batch_size   int DEFAULT 1000
    )
    RETURNS void
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

            RAISE NOTICE 'Batch deleted % rows from %', rows_deleted, full_tbl_name;
        END LOOP;

        RAISE NOTICE 'Total deleted from %: % rows', full_tbl_name, total_deleted;
    END;
    $body$;
    $fn$;

    ----------------------------------------------------------------------
    -- prepare_cleanup_temp_tables
    ----------------------------------------------------------------------
    EXECUTE $fn$
    CREATE OR REPLACE FUNCTION prepare_cleanup_temp_tables()
    RETURNS void
    LANGUAGE plpgsql
    AS $body$
    BEGIN
        BEGIN
            DROP TABLE IF EXISTS tmp_case_data_ids;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Error dropping tmp_case_data_ids: %', SQLERRM;
        END;

        CREATE TEMP TABLE tmp_case_data_ids AS
        SELECT id
        FROM case_data
        WHERE last_modified <= now() - INTERVAL '3 MONTH'
        ORDER BY id ASC;

        RAISE NOTICE 'Created tmp_case_data_ids with % rows',
            (SELECT COUNT(*) FROM tmp_case_data_ids);
    END;
    $body$;
    $fn$;

    ----------------------------------------------------------------------
    -- drop_cleanup_temp_tables
    ----------------------------------------------------------------------
    EXECUTE $fn$
    CREATE OR REPLACE FUNCTION drop_cleanup_temp_tables()
    RETURNS void
    LANGUAGE plpgsql
    AS $body$
    BEGIN
        BEGIN
            DROP TABLE IF EXISTS tmp_case_data_ids;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Error dropping tmp_case_data_ids: %', SQLERRM;
        END;
    END;
    $body$;
    $fn$;

    ----------------------------------------------------------------------
    -- run_safe_deletes
    ----------------------------------------------------------------------
    EXECUTE $fn$
    CREATE OR REPLACE FUNCTION run_safe_deletes(batch_size int DEFAULT 500)
    RETURNS void
    LANGUAGE plpgsql
    AS $body$
    BEGIN
        -- case_users_audit
        IF EXISTS (SELECT 1 FROM tmp_case_data_ids) THEN
            PERFORM safe_delete_where(
                'case_users_audit',
                'case_data_id',
                'case_data_id IN (SELECT id FROM tmp_case_data_ids)',
                batch_size
            );
        ELSE
            RAISE NOTICE 'Skipping case_users_audit: tmp_case_data_ids empty';
        END IF;

        -- case_users
        IF EXISTS (SELECT 1 FROM tmp_case_data_ids) THEN
            PERFORM safe_delete_where(
                'case_users',
                'case_data_id',
                'case_data_id IN (SELECT id FROM tmp_case_data_ids)',
                batch_size
            );
        ELSE
            RAISE NOTICE 'Skipping case_users: tmp_case_data_ids empty';
        END IF;

        -- case_event_significant_items
        IF EXISTS (SELECT 1 FROM tmp_case_data_ids) THEN
            PERFORM safe_delete_where(
                'case_event_significant_items',
                'case_event_id',
                'case_event_id IN (
                    SELECT id FROM case_event
                    WHERE case_data_id IN (SELECT id FROM tmp_case_data_ids)
                )',
                batch_size
            );
        ELSE
            RAISE NOTICE 'Skipping case_event_significant_items: tmp_case_data_ids empty';
        END IF;

        -- case_event
        IF EXISTS (SELECT 1 FROM tmp_case_data_ids) THEN
            PERFORM safe_delete_where(
                'case_event',
                'case_data_id',
                'case_data_id IN (SELECT id FROM tmp_case_data_ids)',
                batch_size
            );
        ELSE
            RAISE NOTICE 'Skipping case_event: tmp_case_data_ids empty';
        END IF;

        -- all_events
        IF EXISTS (SELECT 1 FROM tmp_case_data_ids) THEN
            PERFORM safe_delete_where(
                'all_events',
                'case_id',
                'case_id IN (SELECT id FROM tmp_case_data_ids)',
                batch_size
            );
        ELSE
            RAISE NOTICE 'Skipping all_events: tmp_case_data_ids empty';
        END IF;

        -- case_link (by case_id)
        IF EXISTS (SELECT 1 FROM tmp_case_data_ids) THEN
            PERFORM safe_delete_where(
                'case_link',
                'case_id',
                'case_id IN (SELECT id FROM tmp_case_data_ids)',
                batch_size
            );
        ELSE
            RAISE NOTICE 'Skipping case_link by case_id: tmp_case_data_ids empty';
        END IF;

        -- case_link (by linked_case_id)
        IF EXISTS (SELECT 1 FROM tmp_case_data_ids) THEN
            PERFORM safe_delete_where(
                'case_link',
                'linked_case_id',
                'linked_case_id IN (SELECT id FROM tmp_case_data_ids)',
                batch_size
            );
        ELSE
            RAISE NOTICE 'Skipping case_link by linked_case_id: tmp_case_data_ids empty';
        END IF;

        -- case_data
        IF EXISTS (SELECT 1 FROM tmp_case_data_ids) THEN
            PERFORM safe_delete_where(
                'case_data',
                'id',
                'id IN (SELECT id FROM tmp_case_data_ids)',
                batch_size
            );
        ELSE
            RAISE NOTICE 'Skipping case_data: tmp_case_data_ids empty';
        END IF;
    END;
    $body$;
    $fn$;

    ----------------------------------------------------------------------
    -- 2. RUN PIPELINE
    ----------------------------------------------------------------------
    PERFORM prepare_cleanup_temp_tables();
    PERFORM run_safe_deletes(batch_size);
    PERFORM drop_cleanup_temp_tables();

    ----------------------------------------------------------------------
    -- 3. DROP HELPERS (cleanup)
    ----------------------------------------------------------------------
    EXECUTE 'DROP FUNCTION IF EXISTS safe_delete_query(text, regclass, text, int4)';
    EXECUTE 'DROP FUNCTION IF EXISTS safe_delete_where(regclass, text, text, int4)';
    EXECUTE 'DROP FUNCTION IF EXISTS prepare_cleanup_temp_tables()';
    EXECUTE 'DROP FUNCTION IF EXISTS drop_cleanup_temp_tables()';
    EXECUTE 'DROP FUNCTION IF EXISTS run_safe_deletes(int4)';

    RAISE NOTICE 'cleanup_case_data procedure finished successfully with batch_size=%', batch_size;

END;
$$;
