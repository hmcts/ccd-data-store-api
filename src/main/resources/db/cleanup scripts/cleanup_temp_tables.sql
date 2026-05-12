-- #########################################################################
-- Preview & Cleanup: Backup/Temp-like persistent tables (non-temp schemas)
-- #########################################################################
-- HOW IT WORKS
-- 1) Builds a candidate list by name-patterns (from your list).
-- 2) Extracts a date token from table name and parses it to a DATE.
-- 3) Keeps tables >= size_threshold_mb AND age >= min_age_days (from token).
-- 4) PREVIEW: shows targets with generated DROP SQL.
-- 5) CLEANUP: if dry_run = FALSE, drops those tables.
-- #########################################################################
--How to use it
--	1.	Preview first: set dry_run := TRUE in the params CTE to only list targets and the DROP SQL.
--	2.	Adjust:
--	•	size_threshold_mb (e.g., 100, 500, …)
--	•	min_age_days (e.g., 30, 90, …)
--	•	only_my_tables to restrict to your own objects.
--	•	schema_whitelist if you want to target specific schemas only.
--	3.	To actually drop, set dry_run := FALSE (i.e.TRUE::bool AS dry_run) and rerun the whole block.

DROP TABLE IF EXISTS tmp_drop_targets;

CREATE TEMP TABLE tmp_drop_targets AS
WITH
params AS (
  SELECT
    100::bigint  AS size_threshold_mb,   -- MB
    30::int      AS min_age_days,        -- min age from name token
    TRUE::bool   AS dry_run,             -- TRUE = preview only; set FALSE to drop
    TRUE::bool   AS only_my_tables,      -- restrict to current_user’s tables
    FALSE::bool  AS use_cascade,         -- drop ... CASCADE ?
    ARRAY['public']::text[] AS schema_whitelist, -- only tables from the public schema are considered
    ARRAY[
      -- exact/short names
      'aug_latest',
      'dec_latest',
      'dec_temp_metadata',
      'probate_backup_noc_bulkscan',
      -- families/patterns you provided
      'probate_dtspb_%_case_event',
      'probate_dtspb_%_backup',
      'dtspb_%_probate_draft_case',
      'affected_case_chg%',
      'asylum_case_data_backup_%',
      'asylum_tmp_%',
      'backup_fpl_%',
      'bails_case_data_backup_%',
      'care_supervision_epo_%',
      'case_outcomes_temp_%',
      'chg%_backup',
      'chg%_temp_table',
      'civil_tmp_%',
      'dtspb%_temp_table',
      'dtspb_%',
      'es_incident_tmp_%',
      'financialremedycontested_tmp_%',
      'nfd_tmp_%',
      'nfdiv_case_data_backup_%',
      'probate_data_migration_%',
      'probate_dtspb_%',
      'sptribvs_case_data_backup_%',
      'sscs_case_migration_%',
      'st_cic_case_migration_%',
      'supplementary_data_probate_cases_%',
      'temp_%',
      'tmp_%'
    ]::text[] AS name_patterns
),

candidates AS (
  SELECT
    c.oid,
    n.nspname AS schema_name,
    c.relname AS table_name,
    r.rolname AS owner,
    pg_total_relation_size(c.oid) AS total_bytes,
    pg_table_size(c.oid)          AS table_bytes,
    pg_indexes_size(c.oid)        AS index_bytes,
    c.reltuples::bigint           AS est_rows
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
  JOIN pg_roles r     ON r.oid = c.relowner
  JOIN params p       ON TRUE
  WHERE c.relkind = 'r'
    AND n.nspname NOT LIKE 'pg_%'
    AND n.nspname <> 'information_schema'
    AND (
      cardinality(p.schema_whitelist) = 0
      OR n.nspname = ANY(p.schema_whitelist)
    )
    AND EXISTS (
      SELECT 1
      FROM unnest(p.name_patterns) pat
      WHERE c.relname ILIKE pat
    )
    AND (
      p.only_my_tables IS FALSE
      OR r.rolname = current_user
    )
),

name_tokens AS (
  SELECT
    c.*,
    -- 20240926
    substring(c.table_name from '(\d{8})')              AS token_yyyymmdd,
    -- 26_09_2024
    substring(c.table_name from '(\d{2}_\d{2}_\d{4})')  AS token_dd_mm_yyyy,
    -- 260924 captured as group 2 using regexp_match
    (regexp_match(c.table_name, '(^|_)(\d{6})(_|$)'))[2] AS token_ddmmyy
  FROM candidates c
),

parsed AS (
  SELECT
    nt.*,
    /* keep your underscore format as-is */
    CASE WHEN nt.token_dd_mm_yyyy ~ '^\d{2}_\d{2}_\d{4}$'
         THEN to_date(nt.token_dd_mm_yyyy, 'DD_MM_YYYY')
    END AS parsed_dd_mm_yyyy,

    /* keep your 6-digit ddmmyy as-is */
    CASE WHEN nt.token_ddmmyy ~ '^\d{6}$'
         THEN to_date(nt.token_ddmmyy, 'DDMMYY')
    END AS parsed_ddmmyy,

    /* SMART parse for any 8-digit token: try YYYYMMDD, else DDMMYYYY, else MMDDYYYY */
    CASE
      WHEN nt.token_yyyymmdd ~ '^\d{8}$' THEN
        CASE
          /* YYYYMMDD */
          WHEN substring(nt.token_yyyymmdd from 1 for 4)::int BETWEEN 1900 AND 2099
           AND substring(nt.token_yyyymmdd from 5 for 2)::int BETWEEN 1 AND 12
           AND substring(nt.token_yyyymmdd from 7 for 2)::int BETWEEN 1 AND 31
          THEN to_date(nt.token_yyyymmdd, 'YYYYMMDD')

          /* DDMMYYYY (handles 02102024 → 02/10/2024) */
          WHEN substring(nt.token_yyyymmdd from 1 for 2)::int BETWEEN 1 AND 31
           AND substring(nt.token_yyyymmdd from 3 for 2)::int BETWEEN 1 AND 12
           AND substring(nt.token_yyyymmdd from 5 for 4)::int BETWEEN 1900 AND 2099
          THEN to_date(nt.token_yyyymmdd, 'DDMMYYYY')

          /* MMDDYYYY */
          WHEN substring(nt.token_yyyymmdd from 1 for 2)::int BETWEEN 1 AND 12
           AND substring(nt.token_yyyymmdd from 3 for 2)::int BETWEEN 1 AND 31
           AND substring(nt.token_yyyymmdd from 5 for 4)::int BETWEEN 1900 AND 2099
          THEN to_date(nt.token_yyyymmdd, 'MMDDYYYY')
        END
    END AS parsed_8digit
  FROM name_tokens nt
),

resolved AS (
  SELECT
    p.*,
    /* prefer validated 8-digit parse, then underscore, then 6-digit */
    COALESCE(p.parsed_8digit, p.parsed_dd_mm_yyyy, p.parsed_ddmmyy) AS name_date
  FROM parsed p
),

targets AS (
  SELECT
    r.schema_name, r.table_name, r.owner,
    r.total_bytes, r.table_bytes, r.index_bytes, r.est_rows,
    r.name_date,
    CASE WHEN r.name_date IS NOT NULL THEN (current_date - r.name_date) END AS age_days
  FROM resolved r
  JOIN params p ON TRUE
  WHERE r.total_bytes >= p.size_threshold_mb * 1024 * 1024
    AND r.name_date IS NOT NULL
    AND (current_date - r.name_date) >= p.min_age_days
)

SELECT
  t.schema_name,
  t.table_name,
  t.owner,
  pg_size_pretty(t.total_bytes) AS total_size,
  pg_size_pretty(t.table_bytes) AS data_size,
  pg_size_pretty(t.index_bytes) AS index_size,
  t.est_rows,
  t.name_date,
  t.age_days,
  CASE WHEN p.use_cascade THEN
    format('DROP TABLE %I.%I CASCADE;', t.schema_name, t.table_name)
  ELSE
    format('DROP TABLE %I.%I;', t.schema_name, t.table_name)
  END AS drop_sql,
  -- echo the parameters (prefixed to avoid name collisions)
  p.size_threshold_mb AS param_size_threshold_mb,
  p.min_age_days      AS param_min_age_days,
  p.only_my_tables    AS param_only_my_tables,
  p.dry_run           AS param_dry_run,
  p.use_cascade       AS param_use_cascade
FROM targets t
CROSS JOIN params p
ORDER BY t.total_bytes DESC NULLS LAST;

-- 2) Execute the drops only if param_dry_run = FALSE
DO $exec$
DECLARE
  v_dry_run boolean := TRUE;
  v_cascade boolean := FALSE;
  rec       RECORD;
BEGIN
  SELECT
    COALESCE(bool_or(param_dry_run), TRUE),
    COALESCE(bool_or(param_use_cascade), FALSE)
  INTO v_dry_run, v_cascade
  FROM tmp_drop_targets;

  IF v_dry_run THEN
    RAISE NOTICE 'dry_run = TRUE; no tables will be dropped.';
    RETURN;
  END IF;

  FOR rec IN
    SELECT DISTINCT schema_name, table_name FROM tmp_drop_targets
  LOOP
    IF v_cascade THEN
      EXECUTE format('DROP TABLE %I.%I CASCADE;', rec.schema_name, rec.table_name);
    ELSE
      EXECUTE format('DROP TABLE %I.%I;', rec.schema_name, rec.table_name);
    END IF;
    RAISE NOTICE 'Dropped %.%', rec.schema_name, rec.table_name;
  END LOOP;
END
$exec$;

-- If you want the temp table gone after the run, uncomment:
-- DROP TABLE IF EXISTS tmp_drop_targets;
