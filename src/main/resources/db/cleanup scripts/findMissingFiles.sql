WITH params AS (
  SELECT
    100::bigint  AS size_threshold_mb,
    30::int      AS min_age_days,
    TRUE::bool   AS only_my_tables
),
t AS (
  SELECT
    n.nspname                           AS schema_name,
    c.relname                           AS table_name,
    pg_catalog.pg_get_userbyid(c.relowner) AS owner_name,
    (pg_total_relation_size(c.oid)/1024.0/1024.0)::bigint AS size_mb
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE n.nspname = 'public'
    AND c.relkind = 'r'
    AND c.relname IN (
      'civil_tmp_26_08_2025',
      'sptribvs_case_data_backup_20231207'
    )
),
d AS (
  SELECT
    t.*,
    /* Try multiple date shapes in the name */
    COALESCE(
      /* YYYY_MM_DD or YYYY-MM-DD */
      TO_DATE( (regexp_match(t.table_name, '(\d{4})[_-](\d{2})[_-](\d{2})'))[1]
             ||(regexp_match(t.table_name, '(\d{4})[_-](\d{2})[_-](\d{2})'))[2]
             ||(regexp_match(t.table_name, '(\d{4})[_-](\d{2})[_-](\d{2})'))[3], 'YYYYMMDD'),
      /* DD_MM_YYYY or DD-MM-YYYY */
      TO_DATE( (regexp_match(t.table_name, '(\d{2})[_-](\d{2})[_-](\d{4})'))[3]
             ||(regexp_match(t.table_name, '(\d{2})[_-](\d{2})[_-](\d{4})'))[2]
             ||(regexp_match(t.table_name, '(\d{2})[_-](\d{2})[_-](\d{4})'))[1], 'YYYYMMDD'),
      /* YYYYMMDD */
      TO_DATE( (regexp_match(t.table_name, '(\d{8})'))[1], 'YYYYMMDD')
    ) AS parsed_date
  FROM t
)
SELECT
  d.schema_name,
  d.table_name,
  d.owner_name,
  d.size_mb,
  d.parsed_date,
  (CURRENT_DATE - d.parsed_date)::int AS age_days,
  /* Which filters would exclude it? */
  (SELECT CASE WHEN (SELECT only_my_tables FROM params)
               AND d.owner_name <> CURRENT_USER THEN 'excluded: owner != current_user' END) AS owner_filter,
  (SELECT CASE WHEN d.size_mb < (SELECT size_threshold_mb FROM params)
               THEN 'excluded: size below threshold' END) AS size_filter,
  (SELECT CASE
            WHEN d.parsed_date IS NULL THEN 'excluded: no date parsed from name'
            WHEN (CURRENT_DATE - d.parsed_date) < (SELECT min_age_days FROM params)
                 THEN 'excluded: younger than min_age_days'
          END) AS age_filter
FROM d;
