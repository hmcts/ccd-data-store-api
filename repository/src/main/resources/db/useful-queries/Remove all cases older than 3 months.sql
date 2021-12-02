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

-- List cases by jurisdiction and case [data] containing specific information
SELECT * FROM case_data WHERE Jurisdiction = 'HRS' AND data->>'recordingReference' LIKE 'FUNCTEST%' 