DELETE FROM case_event;
DELETE FROM case_data;
DELETE FROM case_link;


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (1998, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '1504259907353545'
);


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (1999, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '1504259907353537'
);


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (2000, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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

insert into case_link (case_id, linked_case_id, case_type_id, standard_link)
values (1998, 1999, 'TestAddressBookCase', true),
       (1998, 2000, 'TestAddressBookCase', true);
