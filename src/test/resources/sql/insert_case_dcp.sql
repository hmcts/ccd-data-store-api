INSERT INTO case_data (id, created_date, last_modified, case_type_id, jurisdiction, state, security_classification, data, data_classification, reference)
VALUES (1, '2020-04-14 11:29:52.893000', '2020-04-14 11:52:05.190000', 'DCP', 'DCPTest1', 'TODO', 'PUBLIC',
        '{
             "DateField": "2000-10-20",
             "TextField": "Case 1 Text",
             "ComplexField": {
                 "ComplexNestedField": {
                     "NestedNumberField": null,
                     "NestedCollectionTextField": []
                 },
                 "ComplexDateTimeField": "2005-03-28T07:45:30.000"
             },
             "DateTimeField": "1987-11-15T12:30:00.000",
             "CollectionField": [
                 {
                     "id": "4497a96c-3ab2-4b87-a57f-0ff379fa49fb",
                     "value": "2004-03-02T05:06:07.000"
                 },
                 {
                     "id": "7e015142-5806-4446-ad67-c97a3af5ef6d",
                     "value": "2010-09-08T11:12:13.000"
                 }
             ],
             "CollectionComplexDateTime": [
                 {
                     "id": "1c811aa0-116c-45ad-a315-ecd94801a42f",
                     "value": {
                         "DateField": "1963-05-07",
                         "StandardDate": "1999-08-19",
                         "DateTimeField": "2008-04-02T16:37:00.000",
                         "NestedComplex": {
                             "DateField": "1981-02-08",
                             "StandardDate": "2020-02-19",
                             "DateTimeField": "2002-03-04T02:02:00.000",
                             "StandardDateTime": "2007-07-17T07:07:00.000"
                         },
                         "StandardDateTime": "2010-06-17T19:20:00.000"
                     }
                 }
             ]
         }',
        '{
             "DateField": "PUBLIC",
             "TextField": "PUBLIC",
             "ComplexField": {
                 "value": {
                     "ComplexNestedField": {
                         "value": {
                             "NestedNumberField": "PUBLIC",
                             "NestedCollectionTextField": {
                                 "value": [],
                                 "classification": "PUBLIC"
                             }
                         },
                         "classification": "PUBLIC"
                     },
                     "ComplexDateTimeField": "PUBLIC"
                 },
                 "classification": "PUBLIC"
             },
             "DateTimeField": "PUBLIC",
             "CollectionField": {
                 "value": [
                     {
                         "id": "4497a96c-3ab2-4b87-a57f-0ff379fa49fb",
                         "classification": "PUBLIC"
                     },
                     {
                         "id": "7e015142-5806-4446-ad67-c97a3af5ef6d",
                         "classification": "PUBLIC"
                     }
                 ],
                 "classification": "PUBLIC"
             },
             "CollectionComplexDateTime": {
                 "value": [
                     {
                         "id": "1c811aa0-116c-45ad-a315-ecd94801a42f",
                         "value": {
                             "DateField": "PUBLIC",
                             "StandardDate": "PUBLIC",
                             "DateTimeField": "PUBLIC",
                             "NestedComplex": {
                                 "value": {
                                     "DateField": "PUBLIC",
                                     "StandardDate": "PUBLIC",
                                     "DateTimeField": "PUBLIC",
                                     "StandardDateTime": "PUBLIC"
                                 },
                                 "classification": "PUBLIC"
                             },
                             "StandardDateTime": "PUBLIC"
                         }
                     }
                 ],
                 "classification": "PUBLIC"
             }
         }',
        '1587051668000989'
),
(2, '2020-04-21 12:15:26.187000', '2020-04-22 14:45:39.987000', 'DCP', 'DCPTest1', 'TODO', 'PUBLIC',
        '{
             "DateField": "1987-12-01",
             "TextField": "Case 1 Text",
             "ComplexField": {
                 "ComplexNestedField": {
                     "NestedNumberField": null,
                     "NestedCollectionTextField": []
                 },
                 "ComplexDateTimeField": "2004-01-01T00:00:00.000"
             },
             "DateTimeField": "1999-01-01T00:00:00.000",
             "CollectionField": [
                 {
                     "id": "4497a96c-3ab2-4b87-a57f-0ff379fa49fb",
                     "value": "1994-03-02T00:00:00.000"
                 },
                 {
                     "id": "7e015142-5806-4446-ad67-c97a3af5ef6d",
                     "value": "1999-09-08T00:00:00.000"
                 }
             ],
             "CollectionComplexDateTime": [
                 {
                     "id": "1c811aa0-116c-45ad-a315-ecd94801a42f",
                     "value": {
                         "DateField": "1963-05-07",
                         "StandardDate": "1999-08-19",
                         "DateTimeField": "2008-04-02T16:37:00.000",
                         "NestedComplex": {
                             "DateField": "1981-02-08",
                             "StandardDate": "2020-02-19",
                             "DateTimeField": "2002-03-04T02:02:00.000",
                             "StandardDateTime": "2007-07-17T07:07:00.000"
                         },
                         "StandardDateTime": "2010-06-17T19:20:00.000"
                     }
                 }
             ]
         }',
        '{
             "DateField": "PUBLIC",
             "TextField": "PUBLIC",
             "ComplexField": {
                 "value": {
                     "ComplexNestedField": {
                         "value": {
                             "NestedNumberField": "PUBLIC",
                             "NestedCollectionTextField": {
                                 "value": [],
                                 "classification": "PUBLIC"
                             }
                         },
                         "classification": "PUBLIC"
                     },
                     "ComplexDateTimeField": "PUBLIC"
                 },
                 "classification": "PUBLIC"
             },
             "DateTimeField": "PUBLIC",
             "CollectionField": {
                 "value": [
                     {
                         "id": "4497a96c-3ab2-4b87-a57f-0ff379fa49fb",
                         "classification": "PUBLIC"
                     },
                     {
                         "id": "7e015142-5806-4446-ad67-c97a3af5ef6d",
                         "classification": "PUBLIC"
                     }
                 ],
                 "classification": "PUBLIC"
             },
             "CollectionComplexDateTime": {
                 "value": [
                     {
                         "id": "1c811aa0-116c-45ad-a315-ecd94801a42f",
                         "value": {
                             "DateField": "PUBLIC",
                             "StandardDate": "PUBLIC",
                             "DateTimeField": "PUBLIC",
                             "NestedComplex": {
                                 "value": {
                                     "DateField": "PUBLIC",
                                     "StandardDate": "PUBLIC",
                                     "DateTimeField": "PUBLIC",
                                     "StandardDateTime": "PUBLIC"
                                 },
                                 "classification": "PUBLIC"
                             },
                             "StandardDateTime": "PUBLIC"
                         }
                     }
                 ],
                 "classification": "PUBLIC"
             }
         }',
        '1587471326156579'
);
