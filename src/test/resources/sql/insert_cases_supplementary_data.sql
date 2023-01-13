DELETE FROM case_event;
DELETE FROM case_data;

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date, supplementary_data)
VALUES (1001, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
       '2016-06-24 20:44:52.824',
        '{
	          "orgs_assigned_users": {
		            "organisationA": 10
	          }
          }'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date, supplementary_data)
VALUES (1002, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '2016-08-24 20:44:52.824',
        '{
          }'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date, supplementary_data)
VALUES (1003, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '2016-08-24 20:44:53.824',
        '{
            "orgs_assigned_users": {
                "organisationA": 15,
                 "organisationB": 3
          }
        }'
);




INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date, supplementary_data)
VALUES (1004, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
'1504259907353552',
'2016-08-22 20:44:52.824',
'2016-08-24 20:44:52.824',
'2016-08-24 20:44:52.824',
 NULL
);
