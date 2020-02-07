DELETE FROM case_event;
DELETE FROM case_data;
delete from case_users;

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified)
VALUES (1, 'TestAddressBookNoEventAccessToCaseRole', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Parker",
          "PersonAddress": {
            "AddressLine1": "123",
            "AddressLine3": "Hexton",
            "AddressLine2": "Fake Street",
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
       '1504259907353529',
       '2016-06-22 20:44:52.824',
       '2016-06-24 20:44:52.824'
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
        1,
        'TestAddressBookNoEventAccessToCaseRole',
        1,
        'Some comment',
        'The summary',
        'TEST_EVENT_ACCESS',
        'TEST EVENT ACCESS NAME',
        2345,
        'Justin',
        'Smith',
        'CaseCreated',
        'Created a case',
        'PUBLIC',
        '2017-05-09 14:31:43.000000',
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
        1,
        'TestAddressBookNoEventAccessToCaseRole',
        1,
        'Some comment 2',
        'The summary 2',
        'Goodness',
        'GRACIOUS',
        2345,
        'Justin',
        'Smith',
        'state4',
        'Case in state 4',
        'PUBLIC',
        '2017-05-09 15:31:43.000000',
        '{}'
    );



insert into case_users (case_data_id, user_id, case_role)
values (1, 2345, '[TEST-EVENT-ACCESS-ROLE]');
