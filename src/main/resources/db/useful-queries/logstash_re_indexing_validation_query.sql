-- Post re-index validation helper for CCD-4790.
--
-- Use after running logstash_re_indexing_query.sql and before deploying the CCD-4790
-- marked_by_logstash column drop.
--
-- This script proves the DB-side queue status and produces the expected Elasticsearch
-- document counts by index. Elasticsearch still has to be checked separately with
-- _count/_doc requests against the reported index names and document ids.

-- 1. Queue must be zero and stay zero before the column drop is deployed.
SELECT COUNT(*) AS remaining_logstash_queue_rows
FROM public.case_data_logstash_queue;

-- 2. If rows remain, this shows where Logstash has not caught up yet.
SELECT
    cd.jurisdiction,
    cd.case_type_id,
    COUNT(*) AS remaining_queue_rows
FROM public.case_data_logstash_queue q
JOIN public.case_data cd ON cd.id = q.case_data_id
GROUP BY cd.jurisdiction, cd.case_type_id
ORDER BY remaining_queue_rows DESC, cd.jurisdiction, cd.case_type_id;

-- 3. Expected Elasticsearch document counts by index.
-- Logstash writes every non-pointer case to <case_type_id>_cases and also writes
-- cases with data.SearchCriteria to global_search.
WITH expected_documents AS (
    SELECT
        LOWER(cd.case_type_id || '_cases') AS index_name,
        cd.id AS document_id
    FROM public.case_data cd
    WHERE NOT (cd.data = '{}'::jsonb AND cd.state = '')

    UNION ALL

    SELECT
        'global_search' AS index_name,
        cd.id AS document_id
    FROM public.case_data cd
    WHERE NOT (cd.data = '{}'::jsonb AND cd.state = '')
      AND cd.data ? 'SearchCriteria'
)
SELECT
    index_name,
    COUNT(*) AS expected_document_count
FROM expected_documents
GROUP BY index_name
ORDER BY index_name;

-- 4. Sample documents to verify with Elasticsearch _doc lookups.
WITH expected_documents AS (
    SELECT
        LOWER(cd.case_type_id || '_cases') AS index_name,
        cd.id AS document_id,
        cd.reference,
        cd.version
    FROM public.case_data cd
    WHERE NOT (cd.data = '{}'::jsonb AND cd.state = '')

    UNION ALL

    SELECT
        'global_search' AS index_name,
        cd.id AS document_id,
        cd.reference,
        cd.version
    FROM public.case_data cd
    WHERE NOT (cd.data = '{}'::jsonb AND cd.state = '')
      AND cd.data ? 'SearchCriteria'
),
ranked_documents AS (
    SELECT
        *,
        ROW_NUMBER() OVER (PARTITION BY index_name ORDER BY document_id) AS row_number
    FROM expected_documents
)
SELECT
    index_name,
    document_id,
    reference,
    version
FROM ranked_documents
WHERE row_number <= 5
ORDER BY index_name, document_id;
