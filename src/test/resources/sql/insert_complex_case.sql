INSERT INTO case_data (case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES ('TestComplexAddressBookCase', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "Company" : {
            "Name" : "Test Company",
            "PostalAddress" : {
              "AddressLine1" : "123",
              "AddressLine2" : "New Street",
              "AddressLine3" : "Some Town",
              "Country" : "New Country",
              "Postcode" : "PP01 PPQ",
              "Occupant" : {
                "Title" : "Mr",
                "LastName" : "Occupant",
                "FirstName" : "The",
                "MiddleName" : "Test",
                "DateOfBirth" : "01/01/1990",
                "MarritalStatus" : "MARRIAGE",
                "NationalInsuranceNumber" : "AB112233A"
              }
            }
          },
          "OtherInfo" : "Extra Info"
        }','{
          "Company" : {
            "classification" : "PUBLIC",
            "value" : {
              "Name" : "PUBLIC",
              "PostalAddress" : {
                "classification" : "PUBLIC",
                "value" : {
                  "AddressLine1" : "PUBLIC",
                  "AddressLine2" : "PUBLIC",
                  "AddressLine3" : "PUBLIC",
                  "Country" : "PUBLIC",
                  "Postcode" : "PUBLIC",
                  "Occupant" : {
                    "classification" : "PUBLIC",
                    "value" : {
                      "Title" : "PUBLIC",
                      "LastName" : "PUBLIC",
                      "FirstName" : "PUBLIC",
                      "MiddleName" : "PUBLIC",
                      "DateOfBirth" : "PUBLIC",
                      "MarritalStatus" : "PUBLIC",
                      "NationalInsuranceNumber" : "PUBLIC"
                    }
                  }
                }
              }
            }
          },
          "OtherInfo" : "PUBLIC"
        }',
        '1504259907353537'
);

INSERT INTO case_data (case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES ('TestComplexAddressBookCase', 'PROBATE', 'Invalid', 'PUBLIC',
        '{
          "Company" : {
            "Name" : "Test Company",
            "PostalAddress" : {
              "AddressLine1" : "123",
              "AddressLine2" : "New Street",
              "AddressLine3" : "Some Town",
              "Country" : "New Country",
              "Postcode" : "PP01 PPQ",
              "Occupant" : {
                "Title" : "Mr",
                "LastName" : "Occupant",
                "FirstName" : "The",
                "MiddleName" : "Test",
                "DateOfBirth" : "01/01/1990",
                "MarritalStatus" : "MARRIAGE",
                "NationalInsuranceNumber" : "AB112233A"
              }
            }
          },
          "OtherInfo" : "Extra Info"
        }','{
          "Company" : {
            "classification" : "PUBLIC",
            "value" : {
                "Name" : "PUBLIC",
                "PostalAddress" : {
                  "classification" : "PUBLIC",
                  "value" : {
                      "AddressLine1" : "PUBLIC",
                      "AddressLine2" : "PUBLIC",
                      "AddressLine3" : "PUBLIC",
                      "Country" : "PUBLIC",
                      "Postcode" : "PUBLIC",
                      "Occupant" : {
                        "classification" : "PUBLIC",
                        "value" : {
                            "Title" : "PUBLIC",
                            "LastName" : "PUBLIC",
                            "FirstName" : "PUBLIC",
                            "MiddleName" : "PUBLIC",
                            "DateOfBirth" : "PUBLIC",
                            "MarritalStatus" : "PUBLIC",
                            "NationalInsuranceNumber" : "PUBLIC"
                        }
                      }
                  }
                }
            }
          },
          "OtherInfo" : "PUBLIC"
        }',
        '1504259907352539'
);
