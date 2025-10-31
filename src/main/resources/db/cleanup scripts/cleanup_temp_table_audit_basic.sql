-- #########################################################################
-- Report: Backup/Temp-like persistent tables (no pg_stat_file required)
-- #########################################################################

WITH
params AS (
  SELECT
    100::bigint AS size_threshold_mb,  -- change as needed (MB)
    ARRAY[
      -- exact names
      'aug_latest',
      'dec_latest',
      'dec_temp_metadata',
      'probate_backup_noc_bulkscan',
      -- families/patterns from your list
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
    pg_table_size(c.oid)  AS table_bytes,
    pg_indexes_size(c.oid) AS index_bytes,
    c.reltuples,           -- estimated rows
    c.relpages
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
  JOIN pg_roles r ON r.oid = c.relowner
  JOIN params p ON TRUE
  WHERE c.relkind = 'r'
    AND n.nspname NOT LIKE 'pg_%'
    AND n.nspname <> 'information_schema'
    AND EXISTS (
      SELECT 1
      FROM unnest(p.name_patterns) pat
      WHERE c.relname ILIKE pat
    )
),

tab_stats AS (
  SELECT
    relid,
    n_live_tup, n_dead_tup,
    n_tup_ins, n_tup_upd, n_tup_del,
    last_vacuum, last_autovacuum, last_analyze, last_autoanalyze,
    vacuum_count, autovacuum_count, analyze_count, autoanalyze_count
  FROM pg_stat_all_tables
),

name_dates AS (
  -- best-effort tokenization of date-like substrings in table name
  SELECT
    c.*,
    substring(c.table_name from '(\d{8})')                AS token_yyyymmdd,     -- 20240926
    substring(c.table_name from '(\d{2}_\d{2}_\d{4})')    AS token_dd_mm_yyyy,   -- 26_09_2024
    substring(c.table_name from '(^|_)(\d{6})(_|$)')      AS token_ddmmyy       -- 260924
  FROM candidates c
)

SELECT
  nd.schema_name,
  nd.table_name,
  pg_size_pretty(nd.total_bytes) AS total_size,
  pg_size_pretty(nd.table_bytes) AS data_size,
  pg_size_pretty(nd.index_bytes) AS index_size,
  nd.owner,
  nd.reltuples::bigint AS est_rows,
  ts.n_live_tup, ts.n_dead_tup,
  ts.last_vacuum, ts.last_autovacuum, ts.last_analyze, ts.last_autoanalyze,
  COALESCE(NULLIF(nd.token_yyyymmdd, ''),
           NULLIF(nd.token_dd_mm_yyyy, ''),
           NULLIF(nd.token_ddmmyy, '')) AS name_date_token
FROM name_dates nd
LEFT JOIN tab_stats ts ON ts.relid = nd.oid
WHERE nd.total_bytes >= (SELECT size_threshold_mb * 1024 * 1024 FROM params)
ORDER BY nd.total_bytes DESC NULLS LAST;
