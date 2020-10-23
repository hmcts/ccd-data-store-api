#================================================================
@F-125
Feature: F-125 Test to verify Swagger documentation is not broken
#================================================================

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-125.1  @Smoke
Scenario: Must return a positive appropriate response when valid Swagger Internal V1 URL is accessed

    Given an appropriate test context as detailed in the test data source,

     When a request is prepared with appropriate values,
      And it is submitted to call the [Swagger API Internal V1] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected
      And a call [to observe the swagger json content] will get the expected response as in [S-125_Swagger_JSON_V1_Internal].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-125.2  @Smoke
Scenario: Must return a positive appropriate response when valid Swagger External V1 URL is accessed

    Given an appropriate test context as detailed in the test data source,

     When a request is prepared with appropriate values,
      And it is submitted to call the [Swagger API External V1] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected
      And a call [to observe the swagger json content] will get the expected response as in [S-125_Swagger_JSON_V1_External].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-125.3  @Smoke
Scenario: Must return a positive appropriate response when valid Swagger Internal V2 URL is accessed

    Given an appropriate test context as detailed in the test data source,

     When a request is prepared with appropriate values,
      And it is submitted to call the [Swagger API Internal V2] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected
      And a call [to observe the swagger json content] will get the expected response as in [S-125_Swagger_JSON_V2_Internal].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-125.4  @Smoke
Scenario: Must return a positive appropriate response when valid Swagger External V2 URL is accessed

    Given an appropriate test context as detailed in the test data source,

     When a request is prepared with appropriate values,
      And it is submitted to call the [Swagger API External V2] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected
      And a call [to observe the swagger json content] will get the expected response as in [S-125_Swagger_JSON_V2_External].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
