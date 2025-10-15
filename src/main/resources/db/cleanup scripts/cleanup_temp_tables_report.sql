/* =============================================================================
File:        table_cleanup_candidates_report.sql
Created:     2025-10-15
Version:     1.1
Database:    PostgreSQL

Description:
  This script identifies candidate tables (temporary, backup, or staging) for
  cleanup or review based on:
    • Table name patterns (supports % wildcards)
    • Embedded dates in table names (supports multiple formats)
    • Size and age thresholds
    • Ownership (optional restriction to CURRENT_USER)
    • Adds table stats (pg_stat_all_tables) and size breakdown (total/table/index bytes).

  It returns four result sets:
    1. Full diagnostic — all matching tables with filter explanations.
    2. Summary — tables that pass all filters.
    3. Summary count — number of tables that pass all filters.
    4. Failures — tables that fail at least one filter, with a single reason.

Usage:
  Run this script in a PostgreSQL client such as:
    • psql:     \i table_cleanup_candidates_report.sql
    • DBeaver / DataGrip: execute entire script (F5)
    • pgAdmin:  open and execute the script

  To adjust scope:
    - Edit relname_patterns and schema_patterns in the params CTE.
    - Modify thresholds (size_threshold_mb, min_age_days) as needed.
    - Set only_my_tables = FALSE to include all owners.

Dependencies:
  Requires access to pg_class, pg_namespace, pg_stat_all_tables,
  pg_total_relation_size, pg_table_size, and pg_indexes_size.

Output Examples:

Result Set 1 — Full Diagnostic:
  schema_name | table_name                           | owner_name | size_mb | total_bytes | table_bytes | index_bytes | total_size | data_size | index_size | parsed_date | age_days | n_live_tup | n_dead_tup | last_autovacuum     | last_autoanalyze     | exclude_reason
  ------------+--------------------------------------+------------+---------+-------------+-------------+-------------+------------+-----------+------------+-------------+----------+------------+------------+---------------------+---------------------+------------------------------
  public      | civil_tmp_02102024                   | dbadmin    |     200 |   209715200 |   157286400 |    52428800 | 200 MB     | 150 MB    | 50 MB      | 2024-10-02  |      379 |     120000 |       1500 | 2025-10-10 09:33:12 | 2025-10-10 09:40:02 | (null)
  public      | sptribvs_case_data_backup_20231207   | analyst    |      25 |    26214400 |    18874368 |     7340032 | 25 MB      | 18 MB     | 7 MB       | 2023-12-07  |      678 |       9000 |        250 | 2025-09-29 14:05:41 | 2025-09-29 14:06:10 | excluded: size below threshold

Result Set 2 — Passing Summary:
  schema_name | table_name            | owner_name | size_mb | total_bytes | table_bytes | index_bytes | total_size | data_size | index_size | parsed_date | age_days | n_live_tup | n_dead_tup | last_autovacuum     | last_autoanalyze
  ------------+-----------------------+------------+---------+-------------+-------------+-------------+------------+-----------+------------+-------------+----------+------------+------------+---------------------+-------------------
  public      | civil_tmp_02102024    | dbadmin    |     200 |   209715200 |   157286400 |    52428800 | 200 MB     | 150 MB    | 50 MB      | 2024-10-02  |      379 |     120000 |       1500 | 2025-10-10 09:33:12 | 2025-10-10 09:40:02

Result Set 3 — Passing Count:
  passing_table_count
  --------------------
  37

Result Set 4 — Failures (with consolidated reason):
  schema_name | table_name                         | owner_name | size_mb | total_bytes | table_bytes | index_bytes | total_size | data_size | index_size | parsed_date | age_days | exclude_reason
  ------------+------------------------------------+------------+---------+-------------+-------------+-------------+------------+-----------+------------+-------------+----------+-------------------------------
  public      | sptribvs_case_data_backup_20231207 | analyst    |      25 |    26214400 |    18874368 |     7340032 | 25 MB      | 18 MB     | 7 MB       | 2023-12-07  |      678 | excluded: size below threshold

============================================================================= */

DROP TABLE IF EXISTS results;

CREATE TEMP TABLE results AS
WITH params AS (
  SELECT
    100::bigint AS size_threshold_mb,
    30::int     AS min_age_days,
    TRUE::bool  AS only_my_tables,
    ARRAY['public']::text[] AS schema_patterns,
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
    ]::text[] AS relname_patterns
),

/* Base table list with size breakdowns */
t AS (
  SELECT
    c.oid                                   AS relid,
    n.nspname                                AS schema_name,
    c.relname                                AS table_name,
    pg_catalog.pg_get_userbyid(c.relowner)   AS owner_name,
    pg_total_relation_size(c.oid)::bigint    AS total_bytes,
    pg_table_size(c.oid)::bigint             AS table_bytes,
    pg_indexes_size(c.oid)::bigint           AS index_bytes,
    (pg_total_relation_size(c.oid)/1024.0/1024.0)::bigint AS size_mb
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
  JOIN pg_roles r ON r.oid = c.relowner
  JOIN params p ON TRUE
  WHERE c.relkind = 'r'                     -- ordinary tables only
    AND n.nspname NOT LIKE 'pg_%'
    AND n.nspname <> 'information_schema'
    AND EXISTS (
      SELECT 1
      FROM unnest((SELECT schema_patterns FROM params)) AS sp(pattern)
      WHERE n.nspname::text LIKE sp.pattern
    )
    AND EXISTS (
      SELECT 1
      FROM unnest((SELECT relname_patterns FROM params)) AS tp(pattern)
      WHERE c.relname::text LIKE tp.pattern
    )
),

/* Table-level stats (vacuum/analyze/tup counters) */
tab_stats AS (
  SELECT
    relid,
    n_live_tup, n_dead_tup,
    n_tup_ins, n_tup_upd, n_tup_del,
    last_vacuum, last_autovacuum, last_analyze, last_autoanalyze,
    vacuum_count, autovacuum_count, analyze_count, autoanalyze_count
  FROM pg_stat_all_tables
),

/* Join sizes + stats, and parse dates from names */
d AS (
  SELECT
    t.*,
    s.n_live_tup, s.n_dead_tup,
    s.n_tup_ins, s.n_tup_upd, s.n_tup_del,
    s.last_vacuum, s.last_autovacuum, s.last_analyze, s.last_autoanalyze,
    s.vacuum_count, s.autovacuum_count, s.analyze_count, s.autoanalyze_count,
    m.rx AS matched_regex, m.g1, m.g2, m.g3,
    COALESCE(
      CASE WHEN m.rx = '((?:19|20)\d{2})[_-]((?:0[1-9]|1[0-2]))[_-]((?:0[1-9]|[12]\d|3[01]))'
           THEN TO_DATE(m.g1 || m.g2 || m.g3, 'YYYYMMDD') END,
      CASE WHEN m.rx = '((?:0[1-9]|[12]\d|3[01]))[_-]((?:0[1-9]|1[0-2]))[_-]((?:19|20)\d{2})'
           THEN TO_DATE(m.g3 || m.g2 || m.g1, 'YYYYMMDD') END,
      CASE WHEN m.rx = '((?:0[1-9]|[12]\d|3[01]))((?:0[1-9]|1[0-2]))((?:19|20)\d{2})'
           THEN TO_DATE(m.g3 || m.g2 || m.g1, 'YYYYMMDD') END,
      CASE WHEN m.rx = '((?:19|20)\d{2})((?:0[1-9]|1[0-2]))((?:0[1-9]|[12]\d|3[01]))'
           THEN TO_DATE(m.g1 || m.g2 || m.g3, 'YYYYMMDD') END
    ) AS parsed_date
  FROM t
  LEFT JOIN tab_stats s USING (relid)
  LEFT JOIN LATERAL (
    WITH candidates AS (
      SELECT 1 AS ord, '((?:19|20)\d{2})[_-]((?:0[1-9]|1[0-2]))[_-]((?:0[1-9]|[12]\d|3[01]))'::text AS rx
      UNION ALL SELECT 2, '((?:0[1-9]|[12]\d|3[01]))[_-]((?:0[1-9]|1[0-2]))[_-]((?:19|20)\d{2})'
      UNION ALL SELECT 3, '((?:0[1-9]|[12]\d|1[0-2]))((?:0[1-9]|1[0-2]))((?:19|20)\d{2})'
      UNION ALL SELECT 4, '((?:19|20)\d{2})((?:0[1-9]|1[0-2]))((?:0[1-9]|[12]\d|3[01]))'
    )
    SELECT
      c.rx,
      (regexp_match(t.table_name::text, c.rx))[1] AS g1,
      (regexp_match(t.table_name::text, c.rx))[2] AS g2,
      (regexp_match(t.table_name::text, c.rx))[3] AS g3
    FROM candidates c
    WHERE t.table_name::text ~ c.rx
    ORDER BY c.ord
    LIMIT 1
  ) m ON TRUE
)

SELECT
  d.schema_name,
  d.table_name,
  /* raw byte sizes */
  d.total_bytes,
  d.table_bytes,
  d.index_bytes,
  /* pretty sizes for readability */
  pg_size_pretty(d.total_bytes) AS total_size,
  pg_size_pretty(d.table_bytes) AS data_size,
  pg_size_pretty(d.index_bytes) AS index_size,
  d.owner_name,
  d.size_mb,
  d.parsed_date,
  (CURRENT_DATE - d.parsed_date)::int AS age_days,
  /* table stats */
  d.n_live_tup, d.n_dead_tup,
  d.n_tup_ins, d.n_tup_upd, d.n_tup_del,
  d.last_vacuum, d.last_autovacuum, d.last_analyze, d.last_autoanalyze,
  d.vacuum_count, d.autovacuum_count, d.analyze_count, d.autoanalyze_count,
  /* filters (precomputed) */
  (SELECT CASE WHEN (SELECT only_my_tables FROM params)
               AND d.owner_name <> CURRENT_USER
               THEN 'excluded: owner != current_user' END) AS owner_filter,
  (SELECT CASE WHEN d.size_mb < (SELECT size_threshold_mb FROM params)
               THEN 'excluded: size below threshold' END) AS size_filter,
  (SELECT CASE
            WHEN d.parsed_date IS NULL THEN 'excluded: no date parsed from name'
            WHEN (CURRENT_DATE - d.parsed_date) < (SELECT min_age_days FROM params)
                 THEN 'excluded: younger than min_age_days'
          END) AS age_filter
FROM d;

/* ==== Result Set 1: full diagnostic ===================================== */
SELECT *
FROM results
ORDER BY schema_name, table_name;

/* ==== Result Set 2: passing summary ====================================== */
SELECT
  schema_name,
  table_name,
  owner_name,
  size_mb,
  total_bytes, table_bytes, index_bytes,
  pg_size_pretty(total_bytes) AS total_size,
  pg_size_pretty(table_bytes) AS data_size,
  pg_size_pretty(index_bytes) AS index_size,
  parsed_date,
  age_days,
  n_live_tup, n_dead_tup,
  last_autovacuum, last_autoanalyze
FROM results
WHERE owner_filter IS NULL
  AND size_filter IS NULL
  AND age_filter IS NULL
ORDER BY size_mb DESC, age_days DESC;

/* ==== Result Set 3: passing count ======================================== */
SELECT
  COUNT(*) AS passing_table_count
FROM results
WHERE owner_filter IS NULL
  AND size_filter IS NULL
  AND age_filter IS NULL;

/* ==== Result Set 4: failures with consolidated reason ==================== */
SELECT
  schema_name,
  table_name,
  owner_name,
  size_mb,
  total_bytes, table_bytes, index_bytes,
  pg_size_pretty(total_bytes) AS total_size,
  pg_size_pretty(table_bytes) AS data_size,
  pg_size_pretty(index_bytes) AS index_size,
  parsed_date,
  age_days,
  array_to_string(
    ARRAY(
      SELECT x FROM unnest(ARRAY[owner_filter, size_filter, age_filter]) AS x
      WHERE x IS NOT NULL
    ),
    '; '
  ) AS exclude_reason
FROM results
WHERE owner_filter IS NOT NULL
   OR size_filter IS NOT NULL
   OR age_filter IS NOT NULL
ORDER BY size_mb DESC, age_days DESC;

/* Optional tidy-up if you don't need the temp table after viewing results:  */
/* DROP TABLE IF EXISTS results; */
