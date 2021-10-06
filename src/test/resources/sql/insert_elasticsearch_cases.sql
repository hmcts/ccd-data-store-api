DELETE FROM case_data;

-- The data for ElasticsearchIT - loaded in here, but indexed by ElasticsearchIT.initData(embeddedElastic) method from:
-- elasticsearch/data/aat_cases
-- elasticsearch/data/mapper_cases
-- elasticsearch/data/restricted_security_cases
-- elasticsearch/data/security_cases

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (1, 'SECURITY', 'AUTOTEST1', '', 'PUBLIC', '{}', '{}', '1588870649839697');

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (2, 'SECURITY', 'AUTOTEST1', '', 'PUBLIC', '{}', '{}', '1589460125872336');

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (3, 'SECURITY', 'AUTOTEST1', '', 'PUBLIC', '{}', '{}', '1589460099608690');

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (4, 'SECURITY', 'AUTOTEST1', 'PENDING', 'PUBLIC', '{}', '{}', '1589460099608691');

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (5, 'AAT', 'AUTOTEST1', 'TODO', 'PUBLIC', '{}', '{}', '1588866820969121');

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (6, 'AAT', 'AUTOTEST1', 'IN_PROGRESS', 'PUBLIC', '{}', '{}', '1589460056217857');

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (7, 'MAPPER', 'AUTOTEST1', 'TODO', 'PUBLIC', '{}', '{}', '1588870615652827');

