DELETE FROM case_event;
DELETE FROM case_data;

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (2, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "George",
          "PersonLastName": "Roof",
          "PersonAddress": {
            "AddressLine1": "Flat 9",
            "AddressLine2": "2 Hubble Avenue",
            "AddressLine3": "ButtonVillie",
            "Country": "Wales",
            "Postcode": "W11 5DF"
          },
          "D8Document": {
            "document_url": "http://localhost:[port]/documents/fa99ac30-d6e4-4cd8-99ac-30d6e4dcd849",
            "document_binary_url": "http://localhost:[port]/documents/fa99ac30-d6e4-4cd8-99ac-30d6e4dcd849/binary",
            "document_filename": "ExistingDocument.pdf"
          },
          "D8Documents": [
            {
              "id": "existing-item-id-001",
              "value": {
                "documentType": "EXISTING_TYPE",
                "documentFile": {
                  "document_url": "http://localhost:[port]/documents/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                  "document_binary_url": "http://localhost:[port]/documents/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/binary",
                  "document_filename": "ExistingDocument.pdf"
                }
              }
            }
          ]
        }',
        '{
          "PersonFirstName": "PUBLIC",
          "PersonLastName": "PUBLIC",
          "PersonAddress": {
            "classification": "PUBLIC",
            "value": {
              "AddressLine1": "PUBLIC",
              "AddressLine2": "PUBLIC",
              "AddressLine3": "PUBLIC",
              "Country": "PUBLIC",
              "Postcode": "PUBLIC"
            }
          },
          "D8Document": "PUBLIC",
          "D8Documents": {
            "classification": "PUBLIC",
            "value": {
              "existing-item-id-001": {
                "classification": "PUBLIC",
                "value": {
                  "documentType": "PUBLIC",
                  "documentFile": "PUBLIC"
                }
              }
            }
          }
        }',
        '1504259907353545',
        '2016-08-22 20:44:52.824',
        '2016-08-24 20:44:52.824',
        '2016-08-24 20:44:52.824'
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
             2,
             'TestAddressBookCase',
             1,
             'Initial event',
             'Initial summary',
             'TEST_EVENT',
             'TEST EVENT NAME',
             0,
             'Cloud',
             'Strife',
             'CaseCreated',
             'Case Created',
             'PUBLIC',
             '2017-05-09 14:31:43.000000',
             '{}'
         );
