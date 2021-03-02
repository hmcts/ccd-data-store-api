DELETE FROM case_data;
DELETE from case_users;

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (1, 'SECURITY', 'AUTOTEST1', '', 'PUBLIC', '{}', '{}', '1588870649839697');

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (2, 'SECURITY', 'AUTOTEST1', '', 'PUBLIC', '{}', '{}', '1589460125872336');

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (3, 'SECURITY', 'AUTOTEST1', '', 'PUBLIC', '{}', '{}', '1589460099608690');

insert into case_users (case_data_id, user_id, case_role)
values (2, 123, '[CREATOR]');
