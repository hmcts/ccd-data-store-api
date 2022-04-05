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

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (2001, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '9233017909132197'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (2002, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '3522116262568758'
       );


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (2003, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '4504127458172644'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (2004, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '6913605797587333'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (2005, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '2609130232931622'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (2006, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '8256979053075411'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (2007, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '8855462425591410'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, reference)
VALUES (2008, 'TestAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
        '8990926843606105'
       );


insert into case_link (case_id, linked_case_id, case_type_id, standard_link)
values (1998, 1999, 'TestAddressBookCase', true),
       (1998, 2000, 'TestAddressBookCase', true),
       (1998, 2001, 'TestAddressBookCase', false),
       (2002, 2003, 'TestAddressBookCase', true),
       (2002, 2004, 'TestAddressBookCase', true),
       (2002, 2005, 'TestAddressBookCase', true),
       (2002, 2006, 'TestAddressBookCase', true),
       (2002, 2007, 'TestAddressBookCase', true),
       (2002, 2008, 'TestAddressBookCase', false);

