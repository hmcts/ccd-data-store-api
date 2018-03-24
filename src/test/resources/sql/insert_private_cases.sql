DELETE FROM case_event;
DELETE FROM case_data;

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (2, 'TestAddressBookCase', 'PROBATE', 'some-state', 'PRIVATE',
        '{
          "PersonFirstName": "George",
          "PersonLastName": "Roof",
          "PersonAddress": {
            "classification" : "PUBLIC",
            "value" : {
                "AddressLine1": "Flat 9",
                "AddressLine2": "2 Hubble Avenue",
                "AddressLine3": "ButtonVillie",
                "Country": "Wales",
                "Postcode": "WB11DDF"
            }
          }
        }',
        '1504259907353545'
);

