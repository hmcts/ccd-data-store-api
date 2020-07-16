DELETE FROM case_event;
DELETE FROM case_data;

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (1, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Parker",
          "PersonAddress": {
            "AddressLine1": "123",
            "AddressLine2": "Fake Street",
            "AddressLine3": "Hexton",
            "Country": "England",
            "Postcode": "HX08 5TG"
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
       '2016-06-24 20:44:52.824',
       '2016-06-24 20:44:52.824'
);

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
        '1504259907353545',
        '2016-08-22 20:44:52.824',
        '2016-08-24 20:44:52.824',
        '2016-08-24 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (3, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '1504259907353537',
        '2016-08-22 20:44:53.824',
        '2016-08-24 20:44:53.824',
        '2016-08-24 20:44:53.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (4, 'TestAddressBookCase', 'PROBATE', 'Invalid', 'PUBLIC',
        '{
          "PersonFirstName": "An",
          "PersonLastName": "Other",
          "PersonAddress": {
            "AddressLine1": "Some Street",
            "AddressLine2": "Some Town",
            "AddressLine3": "Some City",
            "Country": "Somewhere",
            "Postcode": "SE1 4EE"
          }
        }',
        '1504259907353552'
);


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (17, 'bookcase-default-post-state', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '1557845948403939',
        '2016-08-22 20:44:52.824',
        '2016-08-24 20:44:52.824',
        '2016-08-24 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (18, 'bookcase-default-pre-state-test', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '1557850043804031',
        '2016-08-22 20:44:52.824',
        '2016-08-24 20:44:52.824',
        '2016-08-24 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (14, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PRIVATE',
        '{
          "PersonFirstName": "Angel",
          "PersonLastName": "Morten",
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
        '1504259907353598',
        '2016-08-22 20:44:54.824',
        '2016-08-24 20:44:54.824',
        '2016-08-24 20:44:54.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (15, 'TestAddressBookCase', 'PROBATE', 'some-state', 'PRIVATE',
        '{
          "PersonFirstName": "Anton",
          "PersonLastName": "Foxcatcher",
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
        '3504259907353518',
        '2019-08-22 20:44:54.824',
        '2019-08-24 20:44:54.824',--=
        '2019-08-24 20:44:54.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (5, 'TestAddressBookCaseNoUpdateCaseAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Parker",
          "PersonAddress": {
            "AddressLine1": "123",
            "AddressLine2": "Fake Street",
            "AddressLine3": "Hexton",
            "Country": "England",
            "Postcode": "HX08 5TG"
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
       '1504259907353578'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (6, 'TestAddressBookCaseNoCreateEventAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Parker",
          "PersonAddress": {
            "AddressLine1": "123",
            "AddressLine2": "Fake Street",
            "AddressLine3": "Hexton",
            "Country": "England",
            "Postcode": "HX08 5TG"
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
       '1504259907353586'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (7, 'TestAddressBookCaseNoCreateFieldAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Parker",
          "PersonAddress": {
            "AddressLine1": "123",
            "AddressLine2": "Fake Street",
            "AddressLine3": "Hexton",
            "Country": "England",
            "Postcode": "HX08 5TG"
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
       '1504259907353594'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (8, 'TestAddressBookCaseNoCreateFieldAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Parker",
          "PersonAddress": {
            "AddressLine1": "123",
            "AddressLine2": "Fake Street",
            "AddressLine3": "Hexton",
            "Country": "England",
            "Postcode": "HX08 5TG"
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
       '1504259907353602'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (9, 'TestAddressBookCaseNoReadCaseTypeAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Parker",
          "PersonAddress": {
            "AddressLine1": "123",
            "AddressLine2": "Fake Street",
            "AddressLine3": "Hexton",
            "Country": "England",
            "Postcode": "HX08 5TG"
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
       '1504259907353610'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (10, 'TestAddressBookCaseNoReadFieldAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Parker",
          "PersonAddress": {
            "AddressLine1": "123",
            "AddressLine2": "Fake Street",
            "AddressLine3": "Hexton",
            "Country": "England",
            "Postcode": "HX08 5TG"
          },
          "D8Document": {
            "document_url": "http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1",
            "document_binary_url": "http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1/binary",
            "document_filename": "Seagulls_Square.jpg"
          }
        }',
       '{
         "PersonFirstName": "PRIVATE",
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
       '1504259907353628'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (11, 'TestAddressBookCaseNoReadEventAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Parker",
          "PersonAddress": {
            "AddressLine1": "123",
            "AddressLine2": "Fake Street",
            "AddressLine3": "Hexton",
            "Country": "England",
            "Postcode": "HX08 5TG"
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
       '1504259907353636'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (12, 'TestAddressBookCaseNoReadCaseTypeAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Parker",
          "PersonAddress": {
            "AddressLine1": "123",
            "AddressLine2": "Fake Street",
            "AddressLine3": "Hexton",
            "Country": "England",
            "Postcode": "HX08 5TG"
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
       '1504259907353643'
);


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (13, 'TestAddressBookCaseNoReadFieldAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Peter",
          "PersonLastName": "Pullen",
          "PersonAddress": {
            "AddressLine1": "Governer House",
            "AddressLine2": "1 Puddle Lane",
            "AddressLine3": "London",
            "Country": "England",
            "Postcode": "SE1 4EE"
          },
          "D8Document": {
            "document_url": "http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1",
            "document_binary_url": "http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1/binary",
            "document_filename": "Seagulls_Square.jpg"
          }
        }',
       '{
         "PersonFirstName": "PRIVATE",
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
       '1504259907353651'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (16, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Doe"
        }',
        '{
          "PersonFirstName": "PUBLIC",
          "PersonLastName": "PUBLIC"
        }',
        '1504254784737847',
        '2019-08-22 20:44:53.824',
        '2019-08-24 20:44:53.824',
        '2019-08-24 20:44:53.824'
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
        'TestAddressBookCase',
        1,
        'Some comment',
        'The summary',
        'TEST_EVENT',
        'TEST TRIGGER_EVENT NAME',
        0,
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
        'TestAddressBookCase',
        1,
        'Some comment 2',
        'The summary 2',
        'Goodness',
        'GRACIOUS',
        0,
        'Justin',
        'Smith',
        'state4',
        'Case in state 4',
        'PUBLIC',
        '2017-05-09 15:31:43.000000',
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
        11,
        'TestAddressBookCaseNoReadEventAccess',
        1,
        'Some comment',
        'The summary',
        'TEST_EVENT',
        'TEST TRIGGER_EVENT NAME',
        0,
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
        11,
        'TestAddressBookCaseNoReadEventAccess',
        1,
        'Some comment 2',
        'The summary 2',
        'Goodness',
        'GRACIOUS',
        0,
        'Justin',
        'Smith',
        'state4',
        'Case in state 4',
        'PUBLIC',
        '2017-05-09 15:31:43.000000',
        '{}'
    );
