#===============================================
@F-125
Feature: F-125: Swagger Pages and Open API Specs
#===============================================

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-125.1
Scenario: must show Swagger UI page for Internal V1 APIs

    Given an appropriate test context as detailed in the test data source,

     When a request is prepared with appropriate values,
      And it is submitted to call the [Get Swagger UI Page] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected
      And a call [to observe the swagger json content] will get the expected response as in [S-125_Swagger_JSON_V1_Internal].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-125.2
Scenario: must show Swagger UI page for External V1 APIs

    Given an appropriate test context as detailed in the test data source,

     When a request is prepared with appropriate values,
      And it is submitted to call the [Get Swagger UI Page] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected
      And a call [to observe the swagger json content] will get the expected response as in [S-125_Swagger_JSON_V1_External].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-125.3
Scenario: must show Swagger UI page for Internal V2 APIs

    Given an appropriate test context as detailed in the test data source,

     When a request is prepared with appropriate values,
      And it is submitted to call the [Get Swagger UI Page] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected
      And a call [to observe the swagger json content] will get the expected response as in [S-125_Swagger_JSON_V2_Internal].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-125.4
Scenario: must show Swagger UI page for External V2 APIs

    Given an appropriate test context as detailed in the test data source,

     When a request is prepared with appropriate values,
      And it is submitted to call the [Get Swagger UI Page] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected
      And a call [to observe the swagger json content] will get the expected response as in [S-125_Swagger_JSON_V2_External].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
