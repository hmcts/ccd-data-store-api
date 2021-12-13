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
-- I.E to perform a delete replace 'SELECT *' with 'DELETE'. Replace 'EMPLOYMENT' 
-- with your Jurisdiction.

    SELECT * FROM case_users_audit WHERE case_data_id IN (
        SELECT id FROM case_data WHERE jurisdiction = 'EMPLOYMENT'
    );

    SELECT * FROM case_users WHERE case_data_id IN (
        SELECT id FROM case_data WHERE jurisdiction = 'EMPLOYMENT'
    );

    SELECT * FROM case_event_significant_items WHERE case_event_id IN (
        SELECT id FROM case_event WHERE case_data_id IN (
            SELECT id FROM case_data WHERE jurisdiction = 'EMPLOYMENT'
        )
    );

    SELECT *  FROM case_event WHERE case_data_id IN (
        SELECT id FROM case_data WHERE jurisdiction = 'EMPLOYMENT'
    );

    SELECT id FROM case_event WHERE case_data_id IN (
        SELECT id FROM case_data WHERE jurisdiction = jurisdiction_ref
    );

    SELECT * FROM all_events WHERE case_id IN (
        SELECT id FROM case_data WHERE jurisdiction = 'EMPLOYMENT'
    ) AND case_event_id in (
        SELECT id  FROM case_event WHERE case_data_id IN (
            SELECT id FROM case_data WHERE jurisdiction = 'EMPLOYMENT'
        )
    );

    SELECT * FROM case_data WHERE jurisdiction = 'EMPLOYMENT';

    SELECT COUNT(*) FROM case_data WHERE jurisdiction = 'EMPLOYMENT';
-- =================================================================================

-- List cases older than 3 Months, retaining highest version
SELECT cd.id, cd.case_type_id, cd.version FROM case_data cd INNER JOIN
        (SELECT reference, MAX("version") AS MaxVersion
            FROM case_data
            GROUP BY reference) grouped_cd
    ON cd.reference = grouped_cd.reference
    AND (cd.version != grouped_cd.MaxVersion AND 
		 cd.created_date <= 'now'::timestamp - '3 MONTHS'::INterval)

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

-- Update case_data based on selection criterea    
    update public.case_data cd
    set data= ce.new_data
    from (
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
    ) ce
    where ce.id = cd.id;
    