-- ==========================================
-- Details about tables within ccd-data-store
-- ==========================================
-- PK/FK relationships in tables case_data/case_event
-- case_data pk column = id
-- case_event pk column = id, fk column = case_data_id
-- case_event_significant_items, pk column = id
-- case_users, pk combined columns = case_data_id + user_id + case_role
-- all_events, no pk or fk relationships defined

-- =================================================================================
-- Queries below return data from the various tables within ccd-data-store
-- The select queries are ordered such that they can be used to delete ALL data by a
-- given Jurisdiction. 
-- I.E to perform a delete replace 'SELECT *' with 'DELETE'. Replace 'YOUR_JURISDICTION' 
-- with the name of your Jurisdiction (i.e. 'EMPLOYMENT').

    SELECT * FROM case_users_audit WHERE case_data_id IN (
        SELECT id FROM case_data WHERE jurisdiction = 'YOUR_JURISDICTION'
    );

    SELECT * FROM case_users WHERE case_data_id IN (
        SELECT id FROM case_data WHERE jurisdiction = 'YOUR_JURISDICTION'
    );

    SELECT * FROM case_event_significant_items WHERE case_event_id IN (
        SELECT id FROM case_event WHERE case_data_id IN (
            SELECT id FROM case_data WHERE jurisdiction = 'YOUR_JURISDICTION'
        )
    );

    SELECT *  FROM case_event WHERE case_data_id IN (
        SELECT id FROM case_data WHERE jurisdiction = 'YOUR_JURISDICTION'
    );

    SELECT id FROM case_event WHERE case_data_id IN (
        SELECT id FROM case_data WHERE jurisdiction = jurisdiction_ref
    );

    SELECT * FROM all_events WHERE case_id IN (
        SELECT id FROM case_data WHERE jurisdiction = 'YOUR_JURISDICTION'
    ) AND case_event_id in (
        SELECT id  FROM case_event WHERE case_data_id IN (
            SELECT id FROM case_data WHERE jurisdiction = 'YOUR_JURISDICTION'
        )
    );

    SELECT * FROM case_data WHERE jurisdiction = 'YOUR_JURISDICTION';

    SELECT COUNT(*) FROM case_data WHERE jurisdiction = 'YOUR_JURISDICTION';
-- =================================================================================

-- List cases by case_data.jurisdiction and case_data.[data] containing specific information
    SELECT * FROM case_data WHERE Jurisdiction = 'HRS' AND data->>'recordingReference' LIKE 'FUNCTEST%' 

-- Select case_data.id and amended case_data.[data] based on case_data and case_event criteria
    select
        cd.id,
        jsonb_set(cd.data,'{claimServedDate}'::text[],to_jsonb(concat_ws('',ce.created_date::date)), true 
        ) as new_data
    from 
        public.case_data cd
    join public.case_event ce on cd.id = (
        select id from case_data 
        where case_data.reference =0000000009000009
        and ce.event_id = 'initiateCase'
        and ce.created_date between '1970-01-01' and '1970-01-01')

-- UPDATE case_data based on selection criterea
UPDATE public.case_data cd
set data = jsonb_set(cd.data,'{claimServedDate}'::text[],to_jsonb(concat_ws('',ce.created_date::date)), true ),
    data_classification = jsonb_set(cast(cd.data_classification as jsonb), '{claimServedDate}', '"PUBLIC"', true)
from case_event ce
where cd.id = ce.case_data_id 
	and ce.created_date between '1970-01-01' and '1970-01-01'
	and cd.case_type_id in ('Bristol', 'Leeds', 'LondonCentral', 'LondonEast', 
                     'LondonSouth', 'Manchester', 'MidlandsEast', 'MidlandsWest', 
                     'Newcastle', 'Scotland', 'Wales', 'Watford')
	and ce.event_id = 'generateCorrespondence'


    
-- -Script to re-trigger logstash indexing
-- -Start

DO $$
DECLARE
    batch_size INT := 1000;
    rows_updated INT;
    total_updated INT;
    current_jurisdiction TEXT;
    start_time TIMESTAMP;
    end_time TIMESTAMP;
BEGIN
    RAISE NOTICE 'Starting batch update...';
   
    DROP TABLE IF EXISTS JurisdictionsToIndex;

    -- Create a temp table with jurisdictions sorted by count descending
    CREATE TEMP TABLE JurisdictionsToIndex AS
        SELECT jurisdiction
        FROM (
            SELECT jurisdiction, COUNT(*) AS count
            FROM case_data WHERE jurisdiction = 'DIVORCE'
            GROUP BY jurisdiction
            ORDER BY count DESC
        ) sub;

    -- Loop through each jurisdiction
    FOR current_jurisdiction IN
        SELECT jurisdiction FROM JurisdictionsToIndex
    LOOP
        total_updated := 0;
        start_time := clock_timestamp();
        RAISE NOTICE 'Processing jurisdiction: %', current_jurisdiction;

        LOOP
            -- Batch update for current jurisdiction
            WITH batch AS (
                SELECT id
                FROM case_data
                WHERE marked_by_logstash = true
                  AND jurisdiction = current_jurisdiction
                LIMIT batch_size
                FOR UPDATE SKIP LOCKED
            )
            UPDATE case_data
            SET marked_by_logstash = false
            WHERE id IN (SELECT id FROM batch);

            -- Correctly get number of rows updated
            GET DIAGNOSTICS rows_updated = ROW_COUNT;

            EXIT WHEN rows_updated = 0;
            total_updated := total_updated + rows_updated;
        END LOOP;

        end_time := clock_timestamp();
        RAISE NOTICE 'Jurisdiction %: updated %, Time taken: % seconds',
                     current_jurisdiction, total_updated, end_time - start_time;
    END LOOP;

    RAISE NOTICE 'Batch update complete.';
END $$;


-- -End
