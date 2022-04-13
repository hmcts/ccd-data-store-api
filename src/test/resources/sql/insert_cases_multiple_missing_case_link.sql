DELETE FROM case_event;
DELETE FROM case_data;
DELETE FROM case_link;

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (1, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
          "CaseLinkCollection" : [
          {
            "id": "90a2df83-f256-43ec-aaa0-48e127a44402",
            "value": {
              "CaseReference": "1504259907353537"
            }
          },
          {
            "id": "84e22baf-5bec-4eec-a31f-7a3954efc9c3",
            "value": {
              "CaseReference": "1557845948403939"
            }
          }]
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
          "CaseLinkCollection" : {
            "classification" : "PUBLIC",
            "value" : [ {
              "value" : {
                "CaseReference" : "PUBLIC"
              },
              "id" : "90a2df83-f256-43ec-aaa0-48e127a44402"
            }, {
              "value" : {
                "CaseReference" : "PUBLIC"
              },
              "id" : "84e22baf-5bec-4eec-a31f-7a3954efc9c3"
            } ]
          }
        }',
        '3393027116986763',
        '2016-08-22 20:44:53.824',
        '2016-08-24 20:44:53.824',
        '2016-08-24 20:44:53.824'
);


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (2, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (3, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
          "CaseLinkCollection" : [
          {
            "id": "90a2df83-f256-43ec-aaa0-48e127a44402",
            "value": {
              "CaseReference": "1504259907353552"
            }
          },
          {
            "id": "84e22baf-5bec-4eec-a31f-7a3954efc9c3",
            "value": {
              "CaseReference": "1504254784737847"
            }
          }]
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
           "CaseLinkCollection" : {
              "classification" : "PUBLIC",
              "value" : [ {
                "value" : {
                  "CaseReference" : "PUBLIC"
                },
                "id" : "90a2df83-f256-43ec-aaa0-48e127a44402"
              }, {
                "value" : {
                  "CaseReference" : "PUBLIC"
                },
                "id" : "84e22baf-5bec-4eec-a31f-7a3954efc9c3"
              } ]
            }
        }',
        '1504259907353545',
        '2016-08-22 20:44:53.824',
        '2016-08-24 20:44:53.824',
        '2016-08-24 20:44:53.824'
);


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (4, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '1504259907353552',
        '2016-08-22 20:44:53.824',
        '2016-08-24 20:44:53.824',
        '2016-08-24 20:44:53.824'
);


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (5, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '1557845948403939',
        '2016-08-22 20:44:53.824',
        '2016-08-24 20:44:53.824',
        '2016-08-24 20:44:53.824'
);


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (6, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '1504254784737847',
        '2016-08-22 20:44:53.824',
        '2016-08-24 20:44:53.824',
        '2016-08-24 20:44:53.824'
);
