CREATE OR REPLACE FUNCTION safe_delete_query(
    delete_sql   text,         -- original DELETE, may include USING/aliases
    tbl          regclass,     -- target table
    pk_column    text,         -- PK column of target table
    batch_size   int DEFAULT 1000
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    rows_deleted int;
    total_deleted int := 0;
    full_tbl_name text := tbl::text;
    base_table text;
    alias_name text;
    select_pk_sql text;
    m text[];
BEGIN
    /*
      Parse "DELETE FROM <table> [alias] rest"
      Group 1 = table name
      Group 2 = alias (optional)
      Group 3 = rest of statement (USING/WHERE)
    */
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
    -- m[3] is " rest", including USING/WHERE

    -- Build SELECT DISTINCT pk_val, ctid
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

    <<batch_loop>>
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
        EXIT batch_loop WHEN rows_deleted = 0;

        total_deleted := total_deleted + rows_deleted;

        RAISE NOTICE 'Batch deleted % rows from %', rows_deleted, full_tbl_name;

        INSERT INTO ddl_log(action, table_name, message)
        VALUES ('DELETE', full_tbl_name,
                'Deleted batch of ' || rows_deleted || ' rows from ' || full_tbl_name);
    END LOOP;

    RAISE NOTICE 'Total deleted from %: % rows', full_tbl_name, total_deleted;

    INSERT INTO ddl_log(action, table_name, message)
    VALUES ('DELETE SUMMARY', full_tbl_name,
            'Total deleted ' || total_deleted || ' rows from ' || full_tbl_name);
END;
$$;

CREATE OR REPLACE FUNCTION prepare_cleanup_temp_tables(older_than_months int DEFAULT 3)
RETURNS void AS
$$
BEGIN
    -- Drop temp tables if they already exist
    BEGIN
    DROP TABLE IF EXISTS case_ids_to_remove;
        EXCEPTION WHEN OTHERS THEN
        -- ignore error
    END;

    -- Create log output table if not exists
	CREATE TABLE IF NOT EXISTS ddl_log (
	    log_time TIMESTAMP DEFAULT now(),
	    action TEXT,
	    table_name TEXT,
	    message TEXT
	);

	-- Create temp table of case_type IDs to remove
	EXECUTE format(
        'CREATE TEMP TABLE case_ids_to_remove AS
         SELECT id
         FROM case_data
         WHERE last_modified <= now() - INTERVAL ''%s MONTH''
         ORDER BY id ASC',
         older_than_months
    );

	RAISE NOTICE 'Created temp table case_ids_to_remove for records older than % months with % rows',
        older_than_months, (SELECT COUNT(*) FROM case_ids_to_remove);

END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION drop_cleanup_temp_tables()
RETURNS void AS
$$
BEGIN
    
  BEGIN
    DROP TABLE IF EXISTS case_ids_to_remove;
  EXCEPTION WHEN OTHERS THEN
    -- ignore error
  END;

END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION safe_delete_where(
    tbl          regclass,     -- target table
    pk_column    text,         -- PK column
    where_clause text,         -- just the WHERE condition (without "WHERE")
    batch_size   int DEFAULT 1000
)
RETURNS void
LANGUAGE plpgsql
AS $$
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
        INSERT INTO ddl_log(action, table_name, message)
        VALUES ('DELETE', full_tbl_name,
                'Deleted batch of ' || rows_deleted || ' rows from ' || full_tbl_name);
    END LOOP;

    RAISE NOTICE 'Total deleted from %: % rows', full_tbl_name, total_deleted;
    INSERT INTO ddl_log(action, table_name, message)
    VALUES ('DELETE SUMMARY', full_tbl_name,
            'Total deleted ' || total_deleted || ' rows from ' || full_tbl_name);
END;
$$;

CREATE OR REPLACE FUNCTION run_safe_deletes(batch_size int DEFAULT 500)
RETURNS void AS
$$
BEGIN
    
    -- Case data–related deletions (driven by case_ids_to_remove)

    -- case_users_audit
    IF EXISTS (SELECT 1 FROM case_ids_to_remove) THEN
        PERFORM safe_delete_where(
            'case_users_audit',
            'case_data_id',
            'case_data_id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );
    ELSE
        RAISE NOTICE 'Skipping case_users_audit: case_ids_to_remove empty';
    END IF;

    -- case_users
    IF EXISTS (SELECT 1 FROM case_ids_to_remove) THEN
        PERFORM safe_delete_where(
            'case_users',
            'case_data_id',
            'case_data_id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );
    ELSE
        RAISE NOTICE 'Skipping case_users: case_ids_to_remove empty';
    END IF;

    -- case_event_significant_items
    IF EXISTS (SELECT 1 FROM case_ids_to_remove) THEN
        PERFORM safe_delete_where(
            'case_event_significant_items',
            'case_event_id',
            'case_event_id IN (
                SELECT id FROM case_event
                WHERE case_data_id IN (SELECT id FROM case_ids_to_remove)
            )',
            batch_size
        );
    ELSE
        RAISE NOTICE 'Skipping case_event_significant_items: case_ids_to_remove empty';
    END IF;

    -- case_event
    IF EXISTS (SELECT 1 FROM case_ids_to_remove) THEN
        PERFORM safe_delete_where(
            'case_event',
            'case_data_id',
            'case_data_id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );
    ELSE
        RAISE NOTICE 'Skipping case_event: case_ids_to_remove empty';
    END IF;

    -- all_events
    IF EXISTS (SELECT 1 FROM case_ids_to_remove) THEN
        PERFORM safe_delete_where(
            'all_events',
            'case_id',
            'case_id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );
    ELSE
        RAISE NOTICE 'Skipping all_events: case_ids_to_remove empty';
    END IF;

    -- case_link (by case_id)
    IF EXISTS (SELECT 1 FROM case_ids_to_remove) THEN
        PERFORM safe_delete_where(
            'case_link',
            'case_id',
            'case_id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );
    ELSE
        RAISE NOTICE 'Skipping case_link by case_id: case_ids_to_remove empty';
    END IF;

    -- case_link (by linked_case_id)
    IF EXISTS (SELECT 1 FROM case_ids_to_remove) THEN
        PERFORM safe_delete_where(
            'case_link',
            'linked_case_id',
            'linked_case_id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );
    ELSE
        RAISE NOTICE 'Skipping case_link by linked_case_id: case_ids_to_remove empty';
    END IF;

    -- case_data
    IF EXISTS (SELECT 1 FROM case_ids_to_remove) THEN
        PERFORM safe_delete_where(
            'case_data',
            'id',
            'id IN (SELECT id FROM case_ids_to_remove)',
            batch_size
        );
    ELSE
        RAISE NOTICE 'Skipping case_data: case_ids_to_remove empty';
    END IF;
    
END;
$$ LANGUAGE plpgsql;

--create the various temp tables which form the bulk of the data to clean but ensuring base types are not included
SELECT prepare_cleanup_temp_tables(8);
--deletions based on the temp tables created, in batches of 1000
SELECT run_safe_deletes(1000);
--destroy temp tables created as part of this script
SELECT drop_cleanup_temp_tables();

DROP FUNCTION IF EXISTS safe_delete_where(regclass, text, text, int4);
DROP FUNCTION IF EXISTS prepare_cleanup_temp_tables(int);
DROP FUNCTION IF EXISTS drop_cleanup_temp_tables();
DROP FUNCTION IF EXISTS run_safe_deletes(int4);
