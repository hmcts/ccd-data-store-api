DELETE from case_users;

--  Granted access for cases defined in the insert_elasticsearch_cases.sql

-- reference 1589460125872336
insert into case_users (case_data_id, user_id, case_role)
values (2, 123, '[CREATOR]');

-- reference 1589460099608691
insert into case_users (case_data_id, user_id, case_role)
values (4, 123, '[DEFENDANT]');
