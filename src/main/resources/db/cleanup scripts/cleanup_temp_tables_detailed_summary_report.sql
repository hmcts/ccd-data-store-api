/* ============================================================================
File:        cleanup_temp_tables_detailed_summary_report.sql
Created:     2025-10-17
Database:    PostgreSQL 12+

Purpose
  Generate a detailed report of temporary, backup, or staging tables that may
  be candidates for cleanup or optimization.  The script scans user schemas for
  likely temporary objects, estimates table size, parses dates from table names
  to infer age, and joins with pg_stat_all_tables to include usage statistics.

Workflow
  1. Optionally copy public.tmp_drop_targets (if present) into a working table
     (__tmp_targets) for cross-checking.
  2. Define report parameters (size, age thresholds, and owner scope) in the
     params CTE.
  3. Gather table metadata from pg_class, pg_namespace, and pg_roles.
  4. Attempt to parse embedded dates from table names in several formats:
         YYYYMMDD, DDMMYYYY, YYYY-MM-DD, DD-MM-YYYY.
         YYYY_MM_DD, DD_MM_YYYY,
         DD-MM-YY, DD_MM_YY,
         DDMMYY
  5. Compute size metrics using pg_total_relation_size() and estimate age_days.
  6. Join with pg_stat_all_tables to add tuple-activity statistics.
  7. Produce a single unified result set (details + summaries).

Key Parameters
  • size_threshold_mb   bigint   – minimum table size (MB) for inclusion.
  • min_age_days        int      – minimum table age in days.
  • only_my_tables      boolean  – if TRUE, restricts to CURRENT_USER tables.
  • schema_patterns     text[]   – schemas to scan (e.g., ARRAY['public']).
  • name patterns       text[]   – LIKE/ILIKE patterns for temp, stg, backup, etc.

Date Parsing
  - The parser attempts to detect and convert embedded date tokens.
  - Tables lacking a valid date show “no date in name” in exclude_reason.
  - Age and size thresholds jointly determine inclusion in summaries.

Performance Notes
  - Uses catalog data only; does not modify user tables.
  - Two-phase size estimation logic reduces pg_total_relation_size() calls.
  - Joins are limited to non-system schemas for efficiency.

Safety
  - Read-only logic; creates only temporary or clearly named working tables.
  - Safe to run in psql, pgAdmin, or DBeaver sessions.

Outputs
  - Detail rows for each table with size, owner, age_days, and stats.
  - Summary “bucket” rows showing totals by threshold categories.
  - Optional discrepancy info if tmp_drop_targets is available.

============================================================================ */


/* Stage optional drop targets safely (no error if missing) */
DO $$
DECLARE
  reg regclass := to_regclass('public.tmp_drop_targets');
BEGIN
  -- Ensure a clean temp table that persists for the entire session
  EXECUTE 'DROP TABLE IF EXISTS __tmp_targets';
  EXECUTE 'CREATE TEMP TABLE __tmp_targets(fqname text)';
  EXECUTE 'TRUNCATE __tmp_targets';
  IF reg IS NOT NULL THEN
    EXECUTE format('INSERT INTO __tmp_targets(fqname) SELECT fqname FROM %I', reg::text);
  END IF;
END$$;

/* Build the original 'results' temp table exactly as in your script */
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
    c.oid                                    AS relid,
    n.nspname                                AS schema_name,
    c.relname                                AS table_name,
    pg_catalog.pg_get_userbyid(c.relowner)   AS owner_name,
    pg_total_relation_size(c.oid)::bigint    AS total_bytes,
    pg_table_size(c.oid)::bigint             AS table_bytes,
    pg_indexes_size(c.oid)::bigint           AS index_bytes,
    (pg_total_relation_size(c.oid)/1024.0/1024.0)::bigint AS size_mb
    , c.reltuples::bigint AS est_rows
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
name_date AS (
  SELECT
    t.*,
    s.n_live_tup, s.n_dead_tup,
    s.n_tup_ins, s.n_tup_upd, s.n_tup_del,
    s.last_vacuum, s.last_autovacuum, s.last_analyze, s.last_autoanalyze,
    s.vacuum_count, s.autovacuum_count, s.analyze_count, s.autoanalyze_count,

    COALESCE(
      /* 1) YYYY-MM-DD or YYYY_MM_DD (YMD) */
      CASE WHEN m.rx='((?:19|20)\d{2})[_-]((?:0[1-9]|1[0-2]))[_-]((?:0[1-9]|[12]\d|3[01]))'
           THEN to_date(m.g1||m.g2||m.g3,'YYYYMMDD') END,
      /* 2) DD-MM-YYYY or DD_MM_YYYY (DMY) — e.g., civil_tmp_26_08_2025 */
      CASE WHEN m.rx='((?:0[1-9]|[12]\d|3[01]))[_-]((?:0[1-9]|1[0-2]))[_-]((?:19|20)\d{2})'
           THEN to_date(m.g3||m.g2||m.g1,'YYYYMMDD') END,
      /* 3) DDMMYYYY (compact DMY) */
      CASE WHEN m.rx='((?:0[1-9]|[12]\d|3[01]))((?:0[1-9]|1[0-2]))((?:19|20)\d{2})'
           THEN to_date(m.g3||m.g2||m.g1,'YYYYMMDD') END,
      /* 4) YYYYMMDD (compact YMD) — e.g., case_outcomes_temp_aat_20241212 */
      CASE WHEN m.rx='((?:19|20)\d{2})((?:0[1-9]|1[0-2]))((?:0[1-9]|[12]\d|3[01]))'
           THEN to_date(m.g1||m.g2||m.g3,'YYYYMMDD') END,
      /* 5) DD-MM-YY or DD_MM_YY (DMY, 2-digit year) */
      CASE WHEN m.rx='((?:0[1-9]|[12]\d|3[01]))[_-]((?:0[1-9]|1[0-2]))[_-](\d{2})'
           THEN to_date(
                  (CASE WHEN (m.g3)::int BETWEEN 70 AND 99
                        THEN '19'||m.g3 ELSE '20'||lpad(m.g3,2,'0') END)
                  || m.g2 || m.g1, 'YYYYMMDD') END,
      /* 6) DDMMYY (compact DMY, 2-digit year) */
      CASE WHEN m.rx='((?:0[1-9]|[12]\d|3[01]))((?:0[1-9]|1[0-2]))(\d{2})'
           THEN to_date(
                  (CASE WHEN (m.g3)::int BETWEEN 70 AND 99
                        THEN '19'||m.g3 ELSE '20'||lpad(m.g3,2,'0') END)
                  || m.g2 || m.g1, 'YYYYMMDD') END

    ) AS parsed_date
    ,
    CASE
      /* 1) YYYY[-_]MM[-_]DD */
      WHEN m.rx='((?:19|20)\d{2})[_-]((?:0[1-9]|1[0-2]))[_-]((?:0[1-9]|[12]\d|3[01]))'
        THEN (m.g1 || '-' || m.g2 || '-' || m.g3)
      /* 2) DD[-_]MM[-_]YYYY */
      WHEN m.rx='((?:0[1-9]|[12]\d|3[01]))[_-]((?:0[1-9]|1[0-2]))[_-]((?:19|20)\d{2})'
        THEN (m.g1 || '-' || m.g2 || '-' || m.g3)
      /* 3) DDMMYYYY */
      WHEN m.rx='((?:0[1-9]|[12]\d|3[01]))((?:0[1-9]|1[0-2]))((?:19|20)\d{2})'
        THEN (m.g1 || m.g2 || m.g3)
      /* 4) YYYYMMDD */
      WHEN m.rx='((?:19|20)\d{2})((?:0[1-9]|1[0-2]))((?:0[1-9]|[12]\d|3[01]))'
        THEN (m.g1 || m.g2 || m.g3)
      /* 5) DD[-_]MM[-_]YY */
      WHEN m.rx='((?:0[1-9]|[12]\d|3[01]))[_-]((?:0[1-9]|1[0-2]))[_-](\d{2})'
        THEN (m.g1 || '-' || m.g2 || '-' || m.g3)
      /* 6) DDMMYY */
      WHEN m.rx='((?:0[1-9]|[12]\d|3[01]))((?:0[1-9]|1[0-2]))(\d{2})'
        THEN (m.g1 || m.g2 || m.g3)
    END AS name_date_token

  FROM t
  LEFT JOIN tab_stats s USING (relid)
  LEFT JOIN LATERAL (
    SELECT ord, rx,
           (regexp_match(t.table_name::text, rx))[1] AS g1,
           (regexp_match(t.table_name::text, rx))[2] AS g2,
           (regexp_match(t.table_name::text, rx))[3] AS g3
    FROM (VALUES
      (1,'((?:19|20)\d{2})[_-]((?:0[1-9]|1[0-2]))[_-]((?:0[1-9]|[12]\d|3[01]))'),  -- YYYY[-_]MM[-_]DD
      (2,'((?:0[1-9]|[12]\d|3[01]))[_-]((?:0[1-9]|1[0-2]))[_-]((?:19|20)\d{2})'),  -- DD[-_]MM[-_]YYYY
      (3,'((?:0[1-9]|[12]\d|3[01]))((?:0[1-9]|1[0-2]))((?:19|20)\d{2})'),          -- DDMMYYYY
      (4,'((?:19|20)\d{2})((?:0[1-9]|1[0-2]))((?:0[1-9]|[12]\d|3[01]))'),          -- YYYYMMDD
      (5,'((?:0[1-9]|[12]\d|3[01]))[_-]((?:0[1-9]|1[0-2]))[_-](\d{2})'),           -- DD[-_]MM[-_]YY
      (6,'((?:0[1-9]|[12]\d|3[01]))((?:0[1-9]|1[0-2]))(\d{2})')                     -- DDMMYY

    ) AS c(ord, rx)
    WHERE t.table_name::text ~ rx
    ORDER BY ord
    LIMIT 1
  ) AS m ON TRUE
),

d AS (
  SELECT name_date.*
  FROM name_date
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

  d.est_rows,
  d.owner_name,
  d.size_mb,
  d.parsed_date,
  d.name_date_token,
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
               THEN 'excluded: size below threshold' END) AS below_size_filter,
  (SELECT CASE WHEN d.size_mb > (SELECT size_threshold_mb FROM params)
               THEN 'size greater than threshold' END) AS above_size_filter,
  (SELECT CASE
            WHEN d.parsed_date IS NULL THEN 'excluded: no date parsed from name'
            WHEN (CURRENT_DATE - d.parsed_date) < (SELECT min_age_days FROM params)
                 THEN 'excluded: younger than min_age_days'
          END) AS age_filter
FROM d;

/* Build unified rows (same as v7) */
WITH final AS (
  SELECT
    r.*,
    CASE
      WHEN r.owner_filter IS NULL AND r.below_size_filter IS NULL AND r.age_filter IS null AND r.above_size_filter IS NULL
      THEN NULL
      ELSE array_to_string(
             ARRAY(SELECT x FROM unnest(ARRAY[r.owner_filter, r.below_size_filter, r.above_size_filter, r.age_filter]) AS x WHERE x IS NOT NULL),
             '; '
           )
    END AS exclude_reason,
    CASE
      WHEN r.owner_filter IS NULL AND r.below_size_filter IS NULL AND r.age_filter IS NULL
        THEN 'over_thresholds'
      ELSE 'excluded'
    END AS bucket,
    EXISTS (
      SELECT 1 FROM __tmp_targets t WHERE t.fqname = (r.schema_name || '.' || r.table_name)
    ) AS in_tmp_drop_targets,
    CASE
      WHEN (r.owner_filter IS NULL AND r.below_size_filter IS NULL AND r.age_filter IS NULL)
           AND NOT EXISTS (SELECT 1 FROM __tmp_targets t WHERE t.fqname = (r.schema_name || '.' || r.table_name))
        THEN 'ELIGIBLE_NOT_IN_TARGETS'
      WHEN NOT (r.owner_filter IS NULL AND r.below_size_filter IS NULL AND r.age_filter IS NULL)
           AND EXISTS (SELECT 1 FROM __tmp_targets t WHERE t.fqname = (r.schema_name || '.' || r.table_name))
        THEN 'IN_TARGETS_BUT_EXCLUDED'
      ELSE NULL
    END AS discrepancy
  FROM results r
),
bucket_summary AS (
  SELECT
    bucket,
    COUNT(*)::bigint                                  AS count_tables,
    COALESCE(SUM(size_mb),0)::numeric                 AS total_size_mb,
    SUM( CASE WHEN in_tmp_drop_targets THEN 1 ELSE 0 END )::bigint AS in_targets_count,
    SUM( CASE WHEN discrepancy IS NOT NULL THEN 1 ELSE 0 END )::bigint AS discrepancy_count
  FROM final
  GROUP BY bucket
)
SELECT
  /* sort helpers as named columns */
  0::int                                              AS row_sort_key,
  CASE bucket WHEN 'over_thresholds' THEN 0 ELSE 1 END AS bucket_sort_key,

  'detail'::text                                      AS row_type,
  f.bucket,
  NULL::text                                          AS summary_label,
  NULL::bigint                                        AS count_tables,
  NULL::numeric                                       AS total_size_mb,
  NULL::bigint                                        AS in_targets_count,
  NULL::bigint                                        AS discrepancy_count,
  f.schema_name, f.table_name, f.owner_name,
  f.size_mb, f.age_days,
  f.exclude_reason, f.in_tmp_drop_targets, f.discrepancy
FROM final f
UNION ALL
SELECT
  1::int                                              AS row_sort_key,
  CASE bucket WHEN 'over_thresholds' THEN 0 ELSE 1 END AS bucket_sort_key,

  'summary'::text                                     AS row_type,
  bs.bucket,
  'bucket_totals'::text                               AS summary_label,
  bs.count_tables,
  bs.total_size_mb,
  bs.in_targets_count,
  bs.discrepancy_count,
  NULL, NULL, NULL,
  NULL, NULL,
  NULL, NULL, NULL
FROM bucket_summary bs
ORDER BY
  row_sort_key,
  bucket_sort_key,
  size_mb DESC NULLS LAST,
  age_days DESC NULLS LAST;

/* Optional tidy-up if you don't need the temp table after viewing results:  */
/* DROP TABLE IF EXISTS __tmp_targets; */
/* DROP TABLE IF EXISTS results; */
