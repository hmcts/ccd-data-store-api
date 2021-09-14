@F-201 @ra
Feature: get case

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-201.1
  Scenario: User can not access the case when user has no case role access
    Given a user with [restricted access to create a case J1-CT1-02]
    And a user with [PUBLIC SC ORGANISATION role assignment without case role access]
    And a case that has just been created as in [F-201_CT1]
    When a request is prepared with appropriate values
    And the request [attempts to get case J1-CT1-02]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

  @S-201.2
  Scenario: User can not access the case when Case Role has insufficient SC for Case Type
    Given a user with [restricted access to create a case J1-CT1-02]
    And a user with [PUBLIC SC ORGANISATION role assignment has insufficient SC for Case Type]
    And a case that has just been created as in [F-201_CT1]
    And a successful call [to give user Solicitor1 a PUBLIC CASE role assignment] as in [GRANT_CASE_ROLE_ASSIGNMENT_PUBLIC_SC]
    When a request is prepared with appropriate values
    And the request [attempts to get case J1-CT1-02]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

  @S-201.3
  Scenario: Only fields with SC less than or equal to that of the actor are returned
    Given a user with [restricted access to create a case J1-CT2-01]
    And a user with [PUBLIC SC ORGANISATION role assignment to view the case and only PUBLIC field F3]
    And a case that has just been created as in [F-201_CT2]
    And a successful call [to give user Solicitor1 a PUBLIC CASE role assignment] as in [GRANT_CASE_ROLE_ASSIGNMENT_PUBLIC_SC]
    When a request is prepared with appropriate values
    And the request [attempts to get case J1-CT2-01]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a positive response is received
    Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
    And the response has all other details as expected

    @S-201.4
    Scenario: User can get a case only seeing fields which they have SC access to
      Given a user with [restricted access to create a case J1-CT2-02]
      And a user with [PUBLIC SC ORGANISATION role assignment to view the case and only PUBLIC field F3]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a PRIVATE CASE role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-02]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
      Then the response [contains the PRIVATE field F2 from the CASE role assignment]
      And the response has all other details as expected

    @S-201.7
    Scenario: Has role assignment for this case but it's not in a state that allows them to see it
      Given a user with [restricted access to create a case J1-CT2-03]
      And a user with [PUBLIC SC ORGANISATION role assignment to view the case and only PUBLIC field F3]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a PRIVATE CASE role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_SOLICITOR2]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-03
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
      And the response has all other details as expected


    @S-201.8
    Scenario: Has role assignment for this case but it's not in a state that allows them to see it
      Given a user with [restricted access to create a case J1-CT2-01]
      And a user with [PUBLIC SC ORGANISATION role assignment to view the case and only PUBLIC field F3]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a PRIVATE CASE role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_SOLICITOR2]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-01]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a negative response is received
      And the response has all other details as expected

    @S-201.9
    Scenario: There's a case role but it's got READONLY=N whereas  RoleToAccessProfiles has READONLY = Y
      Given a user with [restricted access to create a case J1-CT6-01]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a PRIVATE CASE role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_SOLICITOR2]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT6-01]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a negative response is received
      And the response has all other details as expected


    @S-201.11
    Scenario: Only fields with SC <= that of the actor are returned.
      Given a user with [restricted access to create a case J1-CT2-04]
      And a user with [PRIVATE SC ORGANISATION role assignment to view the case and fields F2 and F3]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a PRIVATE CASE role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_STAFF1]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-04]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
      Then the response [contains the PRIVATE field F2 from the CASE role assignment]
      And the response has all other details as expected

    @S-201.12
    Scenario: Only fields with SC <= that of the actor are returned.
      Given a user with [restricted access to create a case J1-CT2-04]
      And a user with [RESTRICTED SC ORGANISATION role assignment to view the case and fields F2, F3 and F4]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a RESTRICTED CASE role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_RESTRICTED]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-04]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
      Then the response [contains the PRIVATE field F2 from the CASE role assignment]
      Then the response [contains the PRIVATE field F4 from the CASE role assignment]
      And the response has all other details as expected

    @S-201.13
    Scenario: Only fields with SC <= that of the actor are returned. User has no Region or Location Role Attributes
      Given a user with [restricted access to create a case J1-CT2-05]
      And a user with [RESTRICTED SC ORGANISATION role assignment to view the case and fields F2, F3 and F4]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a RESTRICTED CASE role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_RESTRICTED]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-05]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
      Then the response [contains the PRIVATE field F2 from the CASE role assignment]
      Then the response [contains the PRIVATE field F4 from the CASE role assignment]
      And the response has all other details as expected


    @S-201.14
    Scenario: Only fields with SC <= that of the actor are returned. User has Region Attribute matching that of the case. User has no Location Attribute (but the case does)
      Given a user with [restricted access to create a case J1-CT2-05]
      And a user with [RESTRICTED SC ORGANISATION role assignment to view the case and fields F2, F3 and F4]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a RESTRICTED CASE with region role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_REGION]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-05]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
      Then the response [contains the PRIVATE field F2 from the CASE role assignment]
      Then the response [contains the PRIVATE field F4 from the CASE role assignment]
      And the response has all other details as expected

    @S-201.15
    Scenario: Only fields with SC <= that of the actor are returned. User has Region Attribute matching that of the case. User has no Location Attribute (and neither does the case)
      Given a user with [restricted access to create a case J1-CT2-06]
      And a user with [RESTRICTED SC ORGANISATION role assignment to view the case and fields F2, F3 and F4]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a RESTRICTED CASE with region role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_REGION]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-06]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
      Then the response [contains the PRIVATE field F2 from the CASE role assignment]
      Then the response [contains the PRIVATE field F4 from the CASE role assignment]
      And the response has all other details as expected


    @S-201.16
    Scenario: Only fields with SC <= that of the actor are returned. User has Region Attribute matching that of the case. User has Location Attribute matching that of the case
      Given a user with [restricted access to create a case J1-CT2-05]
      And a user with [RESTRICTED SC ORGANISATION role assignment to view the case and fields F2, F3 and F4]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a RESTRICTED CASE with region and location role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_REGION_LOCATION]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-05]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
      Then the response [contains the PRIVATE field F2 from the CASE role assignment]
      Then the response [contains the PRIVATE field F4 from the CASE role assignment]
      And the response has all other details as expected

    @S-201.17
    Scenario: Only fields with SC <= that of the actor are returned. User has Region Attribute matching that of the case. User has Location Attribute but the case doesn't
      Given a user with [restricted access to create a case J1-CT2-06]
      And a user with [RESTRICTED SC ORGANISATION role assignment to view the case and fields F2, F3 and F4]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a RESTRICTED CASE with region and location role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_REGION_LOCATION]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-06]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
      Then the response [contains the PRIVATE field F2 from the CASE role assignment]
      Then the response [contains the PRIVATE field F4 from the CASE role assignment]
      And the response has all other details as expected

    @S-201.18
    Scenario: User has Region Attribute matching that of the case. User has at least one Location Attribute but none match that of the case
      Given a user with [restricted access to create a case J1-CT2-07]
      And a user with [RESTRICTED SC ORGANISATION role assignment to view the case and fields F2, F3 and F4]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a RESTRICTED CASE with region matching but location not matching with case] as in [GRANT_CASE_ROLE_ASSIGNMENT_LOCATION_DIFF]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-07]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a negative response is received
      And the response has all other details as expected


    @S-201.19
    Scenario: User has at least one Region Attribute but none match that of the case
      Given a user with [restricted access to create a case J1-CT2-08]
      And a user with [RESTRICTED SC ORGANISATION role assignment to view the case and fields F2, F3 and F4]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a RESTRICTED CASE with region not matching with case] as in [GRANT_CASE_ROLE_ASSIGNMENT_REGION_DIFF]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-08]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a negative response is received
      And the response has all other details as expected


    @S-201.20
    Scenario: Has the right AccessProfile but insufficient SC for the case type
      Given a user with [restricted access to create a case J1-CT1-01]
      And a user with [PUBLIC SC ORGANISATION role assignment to view the case and fields]
      And a case that has just been created as in [F-201_CT1]
      And a successful call [to give user Solicitor1 a PUBLIC CASE role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_PUBLIC]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT1-01]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a negative response is received
      And the response has all other details as expected

    @S-201.21
    Scenario: Default SC for case type has been lowered for this case.
      Given a user with [restricted access to create a case J1-CT1-03]
      And a user with [PUBLIC SC ORGANISATION role assignment to view the case and fields]
      And a case that has just been created as in [F-201_CT1]
      And a successful call [to give user Solicitor1 a PUBLIC CASE role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_PUBLIC]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT1-03]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
      And the response has all other details as expected

    @S-201.22
    Scenario: Default SC for case type has been lowered for this case. Default SC for F2 has been lowered for this case.
      Given a user with [restricted access to create a case J1-CT1-04]
      And a user with [PUBLIC SC ORGANISATION role assignment to view the case and fields]
      And a case that has just been created as in [F-201_CT1]
      And a successful call [to give user Solicitor1 a PUBLIC CASE role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_PUBLIC]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT1-04]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
      Then the response [contains the PUBLIC field F2 from the ORGANISATION role assignment]
      And the response has all other details as expected

    @S-201.23
    Scenario: No Access to Case type.
      Given a user with [restricted access to create a case J1-CT2-01]
      And a user with [PUBLIC SC ORGANISATION role assignment to view the case and fields]
      And a case that has just been created as in [F-201_CT1]
      And a successful call [to give user Solicitor1 a PUBLIC CASE role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT_PUBLIC_OTHER]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-01]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a negative response is received
      And the response has all other details as expected
