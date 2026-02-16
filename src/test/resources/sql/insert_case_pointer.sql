DELETE FROM case_event;
DELETE FROM case_data;

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (1, 'PCS', 'PROBATE', '', 'RESTRICTED',
        '{
        }',
       '{
       }',
       '1644062237356399',
       '2016-06-22 20:44:52.824',
       '2016-06-24 20:44:52.824',
       '2016-06-24 20:44:52.824'
);
