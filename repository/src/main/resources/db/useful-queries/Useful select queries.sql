-- PK/FK relationships in tables case_data/case_event
-- case_data pk column = id
-- case_event pk column = id, fk column = case_data_id
-- case_event_significant_items, fk column = case_event_id
-- case_users, pk combined columns = case_data_id + case_role + user_id

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