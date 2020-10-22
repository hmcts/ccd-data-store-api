#================================================================
@F-125
Feature: F-125 Test to verify Swagger documentation is not broken
#================================================================

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-125.1  @Smoke
Scenario: Must return a positive appropriate response when V1 Internal Swagger API is accessed

    Given a call [to observe the swagger UI content] will get the expected response as in [S-125-Swagger-UI-V1-Internal]
    And a call [to observe the swagger json content] will get the expected response as in [S-125-Swagger-JSON-V1-Internal].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-125.2  @Smoke
Scenario: Must return a positive appropriate response when V1 External Swagger API is accessed

    Given a call [to observe the swagger UI content] will get the expected response as in [S-125-Swagger-UI-V1-External]
    And a call [to observe the swagger json content] will get the expected response as in [S-125-Swagger-JSON-V1-External].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-125.3  @Smoke
Scenario: Must return a positive appropriate response when V2 Internal Swagger API is accessed

    Given a call [to observe the swagger UI content] will get the expected response as in [S-125-Swagger-UI-V2-Internal]
    And a call [to observe the swagger json content] will get the expected response as in [S-125-Swagger-JSON-V2-Internal].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-125.4  @Smoke
Scenario: Must return a positive appropriate response when V2 External Swagger API is accessed

    Given a call [to observe the swagger UI content] will get the expected response as in [S-125-Swagger-UI-V2-External]
    And a call [to observe the swagger json content] will get the expected response as in [S-125-Swagger-JSON-V2-External].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
