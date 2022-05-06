DELETE FROM case_event;
DELETE FROM case_data;
DELETE FROM case_link;


INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
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
          },
          "caseNameHmctsInternal" : "Case Name HMCTS Internal 1"
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
          "caseNameHmctsInternal": "PUBLIC"
        }',
        '1504259907353545',
        '2016-01-22 20:44:52.824',
        '2016-01-24 20:44:52.824',
        '2016-01-24 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (1999, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
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
          "caseNameHmctsInternal" : "Case Name: Scenario 1 linked case 1",
          "caseLinks" : [ {
            "id" : "52837798-42c6-43cc-98f6-0895fdba4961",
            "value" : {
              "CaseReference" : "1504259907353545",
              "CaseType" : "TestAddressBookCase",
              "CreatedDateTime" : "2022-04-28T13:26:53.947877",
              "ReasonForLink" : [ {
                "id" : "ffea83f4-3ec1-4be6-b530-e0b0b2a239af",
                "value" : {
                  "Reason" : "Reason 1.1",
                  "OtherDescription" : "OtherDescription 1.1"
                }
              } ]
            }
          } ]
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
          "caseNameHmctsInternal": "PUBLIC",
          "caseLinks": {
            "classification": "PUBLIC",
            "value": [ {
              "id": "52837798-42c6-43cc-98f6-0895fdba4961",
              "value": {
                "CaseReference": "PUBLIC",
                "CaseType": "PUBLIC",
                "CreatedDateTime": "PUBLIC",
                "ReasonForLink": {
                  "classification": "PUBLIC",
                  "value": [ {
                    "id": "ffea83f4-3ec1-4be6-b530-e0b0b2a239af",
                    "value": {
                      "Reason": "PUBLIC",
                      "OtherDescription": "PUBLIC"
                    }
                  } ]
                }
              }
            } ]
          }
        }',
        '1504259907353537',
        '2016-02-22 20:44:52.824',
        '2016-02-24 20:44:52.824',
        '2016-02-24 20:44:52.824'
);

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (2000, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "An",
          "PersonLastName": "Other",
          "PersonAddress": {
            "AddressLine1": "Some Street",
            "AddressLine2": "Some Town",
            "AddressLine3": "Some City",
            "Country": "Somewhere",
            "Postcode": "SE1 4EE"
          },
          "caseNameHmctsInternal" : "Case Name: Scenario 1 linked case 2",
          "caseLinks" : [ {
            "id" : "8d64133f-cde0-4db7-bdbe-6cb767c63d7d",
            "value" : {
              "CaseReference" : "1504259907353545",
              "CaseType" : "TestAddressBookCase",
              "CreatedDateTime" : "2022-04-14T01:46:57.947877",
              "ReasonForLink" : [ {
                "id" : "57bc2066-545e-4020-8365-5cf4512b3c85",
                "value" : {
                  "Reason" : "Reason 1.2.1",
                  "OtherDescription" : "OtherDescription 1.2.1"
                }
              }, {
                "id" : "2f069606-18ca-453a-893f-a32c31443b16",
                "value" : {
                  "Reason" : "Reason 1.2.2",
                  "OtherDescription" : "OtherDescription 1.2.2"
                }
              } ]
            }
          }, {
            "id" : "d0eec7af-4bf0-4a24-9676-1d2d4dc736e6",
            "value" : {
              "CaseReference" : "1504259907353537",
              "CaseType" : "TestAddressBookCase",
              "CreatedDateTime" : "2022-03-24T09:08:15.947877",
              "ReasonForLink" : [ {
                "id" : "b38a2996-3ddb-42fa-85d5-c8b07387e1ae",
                "value" : {
                  "Reason" : "Reason and link ignored in test (wrong case reference)",
                  "OtherDescription" : "OtherDescription"
                }
              } ]
            }
          }, {
            "id" : "ddd50637-1e17-4395-a101-e65b3ed4e634",
            "value" : {
              "CaseReference" : "1504259907353545",
              "CaseType" : "TestAddressBookCase",
              "CreatedDateTime" : "2022-03-24T09:08:15.947877",
              "ReasonForLink" : [ {
                "id" : "02d7b1a5-d5b7-4abd-8991-59ab8c1b4136",
                "value" : {
                  "Reason" : "Reason 1.2.3",
                  "OtherDescription" : "OtherDescription 1.2.3"
               }
              } ]
            }
          } ]
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
          "caseNameHmctsInternal": "PUBLIC",
          "caseLinks": {
            "classification": "PUBLIC",
            "value": [ {
              "id": "8d64133f-cde0-4db7-bdbe-6cb767c63d7d",
              "value": {
                "CaseReference": "PUBLIC",
                "CaseType": "PUBLIC",
                "CreatedDateTime": "PUBLIC",
                "ReasonForLink": {
                  "classification": "PUBLIC",
                  "value": [ {
                    "id": "57bc2066-545e-4020-8365-5cf4512b3c85",
                    "value": {
                      "Reason": "PUBLIC",
                      "OtherDescription": "PUBLIC"
                    }
                  }, {
                    "id": "2f069606-18ca-453a-893f-a32c31443b16",
                    "value": {
                      "Reason": "PUBLIC",
                      "OtherDescription": "PUBLIC"
                    }
                  } ]
                }
              }
            }, {
              "id": "d0eec7af-4bf0-4a24-9676-1d2d4dc736e6",
              "value": {
                "CaseReference": "PUBLIC",
                "CaseType": "PUBLIC",
                "CreatedDateTime": "PUBLIC",
                "ReasonForLink": {
                  "classification": "PUBLIC",
                  "value": [ {
                    "id": "b38a2996-3ddb-42fa-85d5-c8b07387e1ae",
                    "value": {
                      "Reason": "PUBLIC",
                      "OtherDescription": "PUBLIC"
                    }
                  } ]
                }
              }
            }, {
              "id": "ddd50637-1e17-4395-a101-e65b3ed4e634",
              "value": {
                "CaseReference": "PUBLIC",
                "CaseType": "PUBLIC",
                "CreatedDateTime": "PUBLIC",
                "ReasonForLink": {
                  "classification": "PUBLIC",
                  "value": [ {
                    "id": "02d7b1a5-d5b7-4abd-8991-59ab8c1b4136",
                    "value": {
                      "Reason": "PUBLIC",
                      "OtherDescription": "PUBLIC"
                    }
                  } ]
                }
              }
            } ]
          }
        }',
        '1504259907353552',
        '2016-03-22 20:44:52.824',
        '2016-03-24 20:44:52.824',
        '2016-03-24 20:44:52.824'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (2001, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "An",
          "PersonLastName": "Other",
          "PersonAddress": {
            "AddressLine1": "Some Street",
            "AddressLine2": "Some Town",
            "AddressLine3": "Some City",
            "Country": "Somewhere",
            "Postcode": "SE1 4EE"
          },
          "caseNameHmctsInternal" : "Case Name: Scenario 1 linked case 3 (non-standard)",
          "CaseLink1" : {
            "CaseReference" : "1504259907353545"
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
          "caseNameHmctsInternal": "PUBLIC",
          "CaseLink1" : {
            "classification" : "PUBLIC",
            "value" : {
              "CaseReference" : "PUBLIC"
            }
          }
        }',
        '9233017909132197',
        '2016-04-22 20:44:52.824',
        '2016-04-24 20:44:52.824',
        '2016-04-24 20:44:52.824'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
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
          },
          "caseNameHmctsInternal" : "Case Name HMCTS Internal 2"
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
          "caseNameHmctsInternal": "PUBLIC"
        }',
        '3522116262568758',
        '2016-05-22 20:44:52.824',
        '2016-05-24 20:44:52.824',
        '2016-05-24 20:44:52.824'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (2003, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "An",
          "PersonLastName": "Other",
          "PersonAddress": {
            "AddressLine1": "Some Street",
            "AddressLine2": "Some Town",
            "AddressLine3": "Some City",
            "Country": "Somewhere",
            "Postcode": "SE1 4EE"
          },
          "caseNameHmctsInternal" : "Case Name: Scenario 2 linked case 1",
          "caseLinks" : [ {
            "id" : "b3fc92df-6840-4de5-b152-8f936f57f9ff",
            "value" : {
              "CaseReference" : "3522116262568758",
              "CaseType" : "TestAddressBookCase",
              "CreatedDateTime" : "2022-04-28T13:26:53.947877",
              "ReasonForLink" : [ {
                "id" : "0d4ab6d8-bb9c-4b4d-addd-939a45be935e",
                "value" : {
                  "Reason" : "Reason 2.1",
                  "OtherDescription" : "OtherDescription 2.1"
                }
              } ]
            }
          } ]
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
          "caseNameHmctsInternal": "PUBLIC",
          "caseLinks": {
            "classification": "PUBLIC",
            "value": [ {
              "id": "b3fc92df-6840-4de5-b152-8f936f57f9ff",
              "value": {
                "CaseReference": "PUBLIC",
                "CaseType": "PUBLIC",
                "CreatedDateTime": "PUBLIC",
                "ReasonForLink": {
                  "classification": "PUBLIC",
                  "value": [ {
                    "id": "0d4ab6d8-bb9c-4b4d-addd-939a45be935e",
                    "value": {
                      "Reason": "PUBLIC",
                      "OtherDescription": "PUBLIC"
                    }
                  } ]
                }
              }
            } ]
          }
        }',
        '4504127458172644',
        '2016-06-22 20:44:52.824',
        '2016-06-24 20:44:52.824',
        '2016-06-24 20:44:52.824'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (2004, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "An",
          "PersonLastName": "Other",
          "PersonAddress": {
            "AddressLine1": "Some Street",
            "AddressLine2": "Some Town",
            "AddressLine3": "Some City",
            "Country": "Somewhere",
            "Postcode": "SE1 4EE"
          },
          "caseNameHmctsInternal" : "Case Name: Scenario 2 linked case 2",
          "caseLinks" : [ {
            "id" : "4a34d736-4200-4c40-91e4-7cc0ca7ae185",
            "value" : {
              "CaseReference" : "3522116262568758",
              "CaseType" : "TestAddressBookCase",
              "CreatedDateTime" : "2022-04-28T13:26:53.947877",
              "ReasonForLink" : [ {
                "id" : "0d4ab6d8-bb9c-4b4d-addd-939a45be935e",
                "value" : {
                  "Reason" : "Reason 2.2",
                  "OtherDescription" : "OtherDescription 2.2"
                }
              } ]
            }
          } ]
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
          "caseNameHmctsInternal": "PUBLIC",
          "caseLinks": {
            "classification": "PUBLIC",
            "value": [ {
              "id": "4a34d736-4200-4c40-91e4-7cc0ca7ae185",
              "value": {
                "CaseReference": "PUBLIC",
                "CaseType": "PUBLIC",
                "CreatedDateTime": "PUBLIC",
                "ReasonForLink": {
                  "classification": "PUBLIC",
                  "value": [ {
                    "id": "fdb5afbc-5df4-4eb2-a45e-ca313db28ca7",
                    "value": {
                      "Reason": "PUBLIC",
                      "OtherDescription": "PUBLIC"
                    }
                  } ]
                }
              }
            } ]
          }
        }',
        '6913605797587333',
        '2016-07-22 20:44:52.824',
        '2016-07-24 20:44:52.824',
        '2016-07-24 20:44:52.824'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (2005, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "An",
          "PersonLastName": "Other",
          "PersonAddress": {
            "AddressLine1": "Some Street",
            "AddressLine2": "Some Town",
            "AddressLine3": "Some City",
            "Country": "Somewhere",
            "Postcode": "SE1 4EE"
          },
          "caseNameHmctsInternal" : "Case Name: Scenario 2 linked case 3",
          "caseLinks" : [ {
            "id" : "a7a1ae9b-2a99-4c24-99d7-9654872914bb",
            "value" : {
              "CaseReference" : "3522116262568758",
              "CaseType" : "TestAddressBookCase",
              "CreatedDateTime" : "2022-04-28T13:26:53.947877",
              "ReasonForLink" : [ {
                "id" : "532a5abf-44ab-4d76-bda2-4c07ad95d496",
                "value" : {
                  "Reason" : "Reason 2.3",
                  "OtherDescription" : "OtherDescription 2.3"
                }
              } ]
            }
          } ]
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
          "caseNameHmctsInternal": "PUBLIC",
          "caseLinks": {
            "classification": "PUBLIC",
            "value": [ {
              "id": "a7a1ae9b-2a99-4c24-99d7-9654872914bb",
              "value": {
                "CaseReference": "PUBLIC",
                "CaseType": "PUBLIC",
                "CreatedDateTime": "PUBLIC",
                "ReasonForLink": {
                  "classification": "PUBLIC",
                  "value": [ {
                    "id": "532a5abf-44ab-4d76-bda2-4c07ad95d496",
                    "value": {
                      "Reason": "PUBLIC",
                      "OtherDescription": "PUBLIC"
                    }
                  } ]
                }
              }
            } ]
          }
        }',
        '2609130232931622',
        '2016-08-22 20:44:52.824',
        '2016-08-24 20:44:52.824',
        '2016-08-24 20:44:52.824'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (2006, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "An",
          "PersonLastName": "Other",
          "PersonAddress": {
            "AddressLine1": "Some Street",
            "AddressLine2": "Some Town",
            "AddressLine3": "Some City",
            "Country": "Somewhere",
            "Postcode": "SE1 4EE"
          },
          "caseNameHmctsInternal" : "Case Name: Scenario 2 linked case 4",
          "caseLinks" : [ {
            "id" : "445d11db-61d2-49dc-a9b8-f5fef13eb6f9",
            "value" : {
              "CaseReference" : "3522116262568758",
              "CaseType" : "TestAddressBookCase",
              "CreatedDateTime" : "2022-04-28T13:26:53.947877",
              "ReasonForLink" : [ {
                "id" : "cd086c9c-5460-43fc-aecd-ad50fbdc1c26",
                "value" : {
                  "Reason" : "Reason 2.4",
                  "OtherDescription" : "OtherDescription 2.4"
                }
              } ]
            }
          } ]
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
          "caseNameHmctsInternal": "PUBLIC",
          "caseLinks": {
            "classification": "PUBLIC",
            "value": [ {
              "id": "445d11db-61d2-49dc-a9b8-f5fef13eb6f9",
              "value": {
                "CaseReference": "PUBLIC",
                "CaseType": "PUBLIC",
                "CreatedDateTime": "PUBLIC",
                "ReasonForLink": {
                  "classification": "PUBLIC",
                  "value": [ {
                    "id": "cd086c9c-5460-43fc-aecd-ad50fbdc1c26",
                    "value": {
                      "Reason": "PUBLIC",
                      "OtherDescription": "PUBLIC"
                    }
                  } ]
                }
              }
            } ]
          }
        }',
        '8256979053075411',
        '2016-09-22 20:44:52.824',
        '2016-09-24 20:44:52.824',
        '2016-09-24 20:44:52.824'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (2007, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "An",
          "PersonLastName": "Other",
          "PersonAddress": {
            "AddressLine1": "Some Street",
            "AddressLine2": "Some Town",
            "AddressLine3": "Some City",
            "Country": "Somewhere",
            "Postcode": "SE1 4EE"
          },
          "caseNameHmctsInternal" : "Case Name: Scenario 2 linked case 3 (no-access to case-link field)",
          "caseLinks" : [ {
            "id" : "8a9e1fa2-8cd0-4b0c-b927-dd8177ee3203",
            "value" : {
              "CaseReference" : "3522116262568758",
              "CaseType" : "TestAddressBookCase",
              "CreatedDateTime" : "2022-04-28T13:26:53.947877",
              "ReasonForLink" : [ {
                "id" : "e38d44d6-0ab4-4c74-8247-4c9d3c1be3d0",
                "value" : {
                  "Reason" : "Reason 2.3",
                  "OtherDescription" : "OtherDescription 2.3"
                }
              } ]
            }
          } ]
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
          "caseNameHmctsInternal": "PUBLIC",
          "caseLinks": {
            "classification": "PRIVATE",
            "value": [ {
              "id": "8a9e1fa2-8cd0-4b0c-b927-dd8177ee3203",
              "value": {
                "CaseReference": "PUBLIC",
                "CaseType": "PUBLIC",
                "CreatedDateTime": "PUBLIC",
                "ReasonForLink": {
                  "classification": "PUBLIC",
                  "value": [ {
                    "id": "e38d44d6-0ab4-4c74-8247-4c9d3c1be3d0",
                    "value": {
                      "Reason": "PUBLIC",
                      "OtherDescription": "PUBLIC"
                    }
                  } ]
                }
              }
            } ]
          }
        }',
        '1651653562092458',
        '2016-10-22 20:44:52.824',
        '2016-10-24 20:44:52.824',
        '2016-10-24 20:44:52.824'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (2008, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "An",
          "PersonLastName": "Other",
          "PersonAddress": {
            "AddressLine1": "Some Street",
            "AddressLine2": "Some Town",
            "AddressLine3": "Some City",
            "Country": "Somewhere",
            "Postcode": "SE1 4EE"
          },
          "caseNameHmctsInternal" : "Case Name: Scenario 2 linked case 5",
          "caseLinks" : [ {
            "id" : "3a33e74c-46f9-485d-904a-6022de73fbe2",
            "value" : {
              "CaseReference" : "3522116262568758",
              "CaseType" : "TestAddressBookCase",
              "CreatedDateTime" : "2022-04-28T13:26:53.947877",
              "ReasonForLink" : [ {
                "id" : "2245a464-3329-4d44-a3c6-f53dfda0e216",
                "value" : {
                  "Reason" : "Reason 2.5",
                  "OtherDescription" : "OtherDescription 2.5"
                }
              } ]
            }
          } ]
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
          "caseNameHmctsInternal": "PUBLIC",
          "caseLinks": {
            "classification": "PUBLIC",
            "value": [ {
              "id": "3a33e74c-46f9-485d-904a-6022de73fbe2",
              "value": {
                "CaseReference": "PUBLIC",
                "CaseType": "PUBLIC",
                "CreatedDateTime": "PUBLIC",
                "ReasonForLink": {
                  "classification": "PUBLIC",
                  "value": [ {
                    "id": "2245a464-3329-4d44-a3c6-f53dfda0e216",
                    "value": {
                      "Reason": "PUBLIC",
                      "OtherDescription": "PUBLIC"
                    }
                  } ]
                }
              }
            } ]
          }
        }',
        '8855462425591410',
        '2016-11-22 20:44:52.824',
        '2016-11-24 20:44:52.824',
        '2016-11-24 20:44:52.824'
       );

INSERT INTO case_data (id, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference, created_date, last_modified, last_state_modified_date)
VALUES (2009, 'TestAddressBookCaseCaseLinks', 'PROBATE', 'CaseCreated', 'PUBLIC',
        '{
          "PersonFirstName": "An",
          "PersonLastName": "Other",
          "PersonAddress": {
            "AddressLine1": "Some Street",
            "AddressLine2": "Some Town",
            "AddressLine3": "Some City",
            "Country": "Somewhere",
            "Postcode": "SE1 4EE"
          },
          "caseNameHmctsInternal" : "Case Name: Scenario 2 linked case 6 (non-standard)",
          "CaseLink1" : {
            "CaseReference" : "3522116262568758"
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
          "caseNameHmctsInternal": "PUBLIC",
          "CaseLink1" : {
            "classification" : "PUBLIC",
            "value" : {
              "CaseReference" : "PUBLIC"
            }
          }
        }',
        '8990926843606105',
        '2016-12-22 20:44:52.824',
        '2016-12-24 20:44:52.824',
        '2016-12-24 20:44:52.824'
       );


insert into case_link (case_id, linked_case_id, case_type_id, standard_link)
values -- scenario 1
       (1999, 1998, 'TestAddressBookCase', true),
       (2000, 1998, 'TestAddressBookCase', true),
       (2001, 1998, 'TestAddressBookCase', false), -- non-standard
       -- scenario 2
       (2003, 2002, 'TestAddressBookCase', true),
       (2004, 2002, 'TestAddressBookCase', true),
       (2005, 2002, 'TestAddressBookCase', true),
       (2006, 2002, 'TestAddressBookCase', true),
       (2007, 2002, 'TestAddressBookCase', true), -- hidden standard case link field
       (2008, 2002, 'TestAddressBookCase', true),
       (2009, 2002, 'TestAddressBookCase', false); -- non-standard

