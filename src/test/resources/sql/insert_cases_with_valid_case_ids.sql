DELETE FROM case_event;
DELETE FROM case_data;

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (7578590391163133, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
       '7578590391163133',
       '2016-06-22 20:44:52.824',
       '2016-06-24 20:44:52.824',
       '2016-06-24 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (6375837333991692, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '6375837333991692',
        '2016-08-22 20:44:52.824',
        '2016-08-24 20:44:52.824',
        '2016-08-24 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (1983927457663329, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '1983927457663329',
        '2016-08-22 20:44:53.824',
        '2016-08-24 20:44:53.824',
        '2016-08-24 20:44:53.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (4909408736740613, 'TestAddressBookCase', 'PROBATE', 'Invalid', 'PUBLIC',
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
        '4909408736740613'
);


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (0812226188793136, 'bookcase-default-post-state', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '0812226188793136',
        '2016-08-22 20:44:52.824',
        '2016-08-24 20:44:52.824',
        '2016-08-24 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (8936117598299935, 'bookcase-default-pre-state-test', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '8936117598299935',
        '2016-08-22 20:44:52.824',
        '2016-08-24 20:44:52.824',
        '2016-08-24 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (6820370635029103, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PRIVATE',
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
        '6820370635029103',
        '2016-08-22 20:44:54.824',
        '2016-08-24 20:44:54.824',
        '2016-08-24 20:44:54.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (2406747416264390, 'TestAddressBookCase', 'PROBATE', 'some-state', 'PRIVATE',
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
        '2406747416264390',
        '2019-08-22 20:44:54.824',
        '2019-08-24 20:44:54.824',--=
        '2019-08-24 20:44:54.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (0382067683780388, 'TestAddressBookCaseNoUpdateCaseAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
       '0382067683780388'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (8669769515614407, 'TestAddressBookCaseNoCreateEventAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
       '8669769515614407'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (7878572516474410, 'TestAddressBookCaseNoCreateFieldAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
       '7878572516474410'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (1435318436872880, 'TestAddressBookCaseNoCreateFieldAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
       '1435318436872880'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (5778924340534288, 'TestAddressBookCaseNoReadCaseTypeAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
       '5778924340534288'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (9407256414121846, 'TestAddressBookCaseNoReadFieldAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
       '9407256414121846'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (6224186984843239, 'TestAddressBookCaseNoReadEventAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
       '6224186984843239'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (9558893805689374, 'TestAddressBookCaseNoReadCaseTypeAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
       '9558893805689374'
);


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (2924382960536937, 'TestAddressBookCaseNoReadFieldAccess', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
       '2924382960536937'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (7083640672772496, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "Janet",
          "PersonLastName": "Doe"
        }',
        '{
          "PersonFirstName": "PUBLIC",
          "PersonLastName": "PUBLIC"
        }',
        '7083640672772496',
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
        7578590391163133,
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
         7578590391163133,
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
        6224186984843239,
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
        6224186984843239,
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
