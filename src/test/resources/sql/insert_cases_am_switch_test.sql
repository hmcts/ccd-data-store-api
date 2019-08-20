DELETE FROM case_event;
DELETE FROM case_data;

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified)
VALUES (465, 'DIVORCE', 'CMC', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Parker",
          "PersonAddress": {
            "AddressLine1": "123",
            "AddressLine2": "Fake Street",
            "AddressLine3": "Hexton",
            "Country": "England",
            "Postcode": "HX08 UTG"
          },
          "D8Document": {
            "document_url": "http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1",
            "document_binary_url": "http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1/binary",
            "document_filename": "Seagulls_Square.jpg"
          }
        }',
       '{
         "PersonFirstName": "PUBLIC",
         "PersonLastName": "PUBLIC",
         "PersonAddress": {
           "classification" : "PUBLIC",
           "value" : {
             "AddressLine1": "PUBLIC",
             "AddressLine2": "PUBLIC",
             "AddressLine3": "PUBLIC",
             "Country": "PUBLIC",
             "Postcode": "PUBLIC"
           }
         },
         "D8Document": "PUBLIC"
       }',
       '1234123456781236',
       '2019-08-17 20:44:52.824',
       '2019-08-18 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified)
VALUES (364, 'CMC', 'CMC', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Ashwin",
          "PersonLastName": "Kettay",
          "PersonAddress": {
            "AddressLine1": "153",
            "AddressLine2": "Fake Street",
            "AddressLine3": "Hexton",
            "Country": "England",
            "Postcode": "HX08 UTG"
          },
          "D8Document": {
            "document_url": "http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1",
            "document_binary_url": "http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1/binary",
            "document_filename": "Seagulls_Square.jpg"
          }
        }',
       '{
         "PersonFirstName": "PUBLIC",
         "PersonLastName": "PUBLIC",
         "PersonAddress": {
           "classification" : "PUBLIC",
           "value" : {
             "AddressLine1": "PUBLIC",
             "AddressLine2": "PUBLIC",
             "AddressLine3": "PUBLIC",
             "Country": "PUBLIC",
             "Postcode": "PUBLIC"
           }
         },
         "D8Document": "PUBLIC"
       }',
       '1234123456781239',
       '2019-08-17 20:44:52.824',
       '2019-08-18 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified)
VALUES (798, 'PROBATE', 'CMC', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "George",
          "PersonLastName": "Roof",
          "PersonAddress": {
            "AddressLine1": "Flat 9",
            "AddressLine2": "2 Hubble Avenue",
            "AddressLine3": "ButtonVillie",
            "Country": "Wales",
            "Postcode": "W11 5DF"
          }
        }',
        '{
          "PersonFirstName": "PUBLIC",
          "PersonLastName": "PUBLIC",
          "PersonAddress": {
            "classification" : "PUBLIC",
            "value" : {
              "AddressLine1": "PUBLIC",
              "AddressLine2": "PUBLIC",
              "AddressLine3": "PUBLIC",
              "Country": "PUBLIC",
              "Postcode": "PUBLIC"
            }
          },
          "D8Document": "PUBLIC"
        }',
        '1234123456781237',
        '2019-08-17 20:44:52.824',
        '2019-08-18 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified)
VALUES (718, 'CR', 'CMC', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Senthil",
          "PersonLastName": "Nathan",
          "PersonAddress": {
            "AddressLine1": "Flat 12",
            "AddressLine2": "2 Hubble Avenue",
            "AddressLine3": "ButtonVillie",
            "Country": "Wales",
            "Postcode": "W11 5DF"
          }
        }',
        '{
          "PersonFirstName": "PUBLIC",
          "PersonLastName": "PUBLIC",
          "PersonAddress": {
            "classification" : "PUBLIC",
            "value" : {
              "AddressLine1": "PUBLIC",
              "AddressLine2": "PUBLIC",
              "AddressLine3": "PUBLIC",
              "Country": "PUBLIC",
              "Postcode": "PUBLIC"
            }
          },
          "D8Document": "PUBLIC"
        }',
        '1234123456781231',
        '2019-08-17 20:44:52.824',
        '2019-08-18 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified)
VALUES (132, 'FR', 'CMC', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Peter",
          "PersonLastName": "Pullen",
          "PersonAddress": {
            "AddressLine1": "Governer House",
            "AddressLine2": "1 Puddle Lane",
            "AddressLine3": "London",
            "Country": "England",
            "Postcode": "SE1 4EE"
          }
        }',
        '{
          "PersonFirstName": "PUBLIC",
          "PersonLastName": "PUBLIC",
          "PersonAddress": {
            "classification" : "PUBLIC",
            "value" : {
              "AddressLine1": "PUBLIC",
              "AddressLine2": "PUBLIC",
              "AddressLine3": "PUBLIC",
              "Country": "PUBLIC",
              "Postcode": "PUBLIC"
            }
          }
        }',
        '1234123456781238',
        '2019-08-17 20:44:53.824',
        '2019-08-18 20:44:53.824'
);


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified)
VALUES (152, 'TEST', 'CMC', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "George",
          "PersonLastName": "Roof",
          "PersonAddress": {
            "AddressLine1": "Flat 9",
            "AddressLine2": "2 Hubble Avenue",
            "AddressLine3": "ButtonVillie",
            "Country": "Wales",
            "Postcode": "W11 5DF"
          }
        }',
        '{
          "PersonFirstName": "PUBLIC",
          "PersonLastName": "PUBLIC",
          "PersonAddress": {
            "classification" : "PUBLIC",
            "value" : {
              "AddressLine1": "PUBLIC",
              "AddressLine2": "PUBLIC",
              "AddressLine3": "PUBLIC",
              "Country": "PUBLIC",
              "Postcode": "PUBLIC"
            }
          },
          "D8Document": "PUBLIC"
        }',
        '1234123456781212',
        '2019-08-17 20:44:52.824',
        '2019-08-18 20:44:52.824'
);

INSERT INTO case_event (
        case_data_id,
        case_type_id,
        case_type_version,
        description,
        summary,
        event_id,
        event_name,
        user_id,
        user_first_name,
        user_last_name,
        state_id,
        state_name,
        security_classification,
        created_date,
        data
    ) VALUES (
        465,
        'DIVORCE',
        1,
        'Some comment',
        'The summary',
        'TEST_EVENT',
        'TEST TRIGGER_EVENT NAME',
        132,
        'Justin',
        'Smith',
        'CaseCreated',
        'Created a case',
        'PUBLIC',
        '2019-08-18 14:31:43.000000',
        '{}'
    );

    INSERT INTO case_event (
        case_data_id,
        case_type_id,
        case_type_version,
        description,
        summary,
        event_id,
        event_name,
        user_id,
        user_first_name,
        user_last_name,
        state_id,
        state_name,
        security_classification,
        created_date,
        data
    ) VALUES (
        364,
        'CMC',
        1,
        'Some comment',
        'The summary',
        'TEST_EVENT',
        'TEST TRIGGER_EVENT NAME',
        132,
        'JustinK',
        'SmithY',
        'CaseCreated',
        'Created a case',
        'PUBLIC',
        '2019-08-18 14:31:43.000000',
        '{}'
    );

INSERT INTO case_event (
        case_data_id,
        case_type_id,
        case_type_version,
        description,
        summary,
        event_id,
        event_name,
        user_id,
        user_first_name,
        user_last_name,
        state_id,
        state_name,
        security_classification,
        created_date,
        data
    ) VALUES (
        798,
        'PROBATE',
        1,
        'Some comment 2',
        'The summary 2',
        'Goodness',
        'GRACIOUS',
        132,
        'Justin',
        'Smith',
        'state4',
        'Case in state 4',
        'PUBLIC',
        '2019-08-18 15:31:43.000000',
        '{}'
    );

INSERT INTO case_event (
        case_data_id,
        case_type_id,
        case_type_version,
        description,
        summary,
        event_id,
        event_name,
        user_id,
        user_first_name,
        user_last_name,
        state_id,
        state_name,
        security_classification,
        created_date,
        data
    ) VALUES (
        718,
        'CR',
        1,
        'Some comment 2',
        'The summary 2',
        'Goodness',
        'GRACIOUS',
        132,
        'JustinA',
        'SmithB',
        'state4',
        'Case in state 4',
        'PUBLIC',
        '2019-08-18 15:31:43.000000',
        '{}'
    );

INSERT INTO case_event (
        case_data_id,
        case_type_id,
        case_type_version,
        description,
        summary,
        event_id,
        event_name,
        user_id,
        user_first_name,
        user_last_name,
        state_id,
        state_name,
        security_classification,
        created_date,
        data
    ) VALUES (
        132,
        'FR',
        1,
        'Some comment',
        'The summary',
        'TEST_EVENT',
        'TEST TRIGGER_EVENT NAME',
        132,
        'Justin',
        'Smith',
        'CaseCreated',
        'Created a case',
        'PUBLIC',
        '2019-08-18 14:31:43.000000',
        '{}'
    );

INSERT INTO case_event (
        case_data_id,
        case_type_id,
        case_type_version,
        description,
        summary,
        event_id,
        event_name,
        user_id,
        user_first_name,
        user_last_name,
        state_id,
        state_name,
        security_classification,
        created_date,
        data
    ) VALUES (
        152,
        'TEST',
        1,
        'Some comment 2',
        'The summary 2',
        'Goodness',
        'GRACIOUS',
        1324,
        'Sam',
        'Smith',
        'state4',
        'Case in state 4',
        'PUBLIC',
        '2019-08-18 15:31:43.000000',
        '{}'
    );
