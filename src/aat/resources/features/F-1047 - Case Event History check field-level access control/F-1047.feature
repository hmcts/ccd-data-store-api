@F-1047 @crud
Feature: F-1047: Public case event history enforces case-field read access

  # The local event-list query omits snapshots. FT_Decentralisation supplies populated history.
  # TextField and _stubProcessedBy are PUBLIC: differences below must come from field ACLs.
  # BEFTA compares response maps strictly, so an extra denied field fails the scenario.
  # S-1047.4 requires the nested history fixture in ccd-test-stubs-service.
  # Only the solicitor reader role has access to these extra roots; the flat scenarios remain unchanged.

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1047.1
  Scenario: A caseworker with read access to both fields sees the complete historical snapshot
    Given a successful call [to start decentralised case creation] as in [F1047_CreateCase_Token],
    And a successful call [to create a decentralised case as a citizen] as in [F1047_CreateCase],
    And a user with [an active profile in CCD],
    And a user with [read access to both historical case fields],
    When a request is prepared with appropriate values,
    And the request [contains the reference of the case just created],
    And it is submitted to call the [Retrieve audit events by case ID] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains both readable fields in the historical snapshot],
    And the response has all other details as expected.

  @S-1047.2
  Scenario: A citizen sees the readable field and cannot retrieve the denied field from history
    Given a successful call [to start decentralised case creation] as in [F1047_CreateCase_Token],
    And a successful call [to create a decentralised case as a citizen] as in [F1047_CreateCase],
    And a successful call [to confirm the same historical snapshot contains both fields for an authorised caseworker] as in [F1047_GetHistory_AsMaster],
    And a user with [an active profile in CCD],
    And a user with [read access to TextField but no read access to _stubProcessedBy],
    When a request is prepared with appropriate values,
    And the request [contains the reference of the case just created],
    And it is submitted to call the [Retrieve audit events by case ID] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains TextField and omits _stubProcessedBy from the historical snapshot],
    And the response has all other details as expected.

  @S-1047.3
  Scenario: A reader with case and event access but no field access receives an empty snapshot
    Given a successful call [to start decentralised case creation] as in [F1047_CreateCase_Token],
    And a successful call [to create a decentralised case as a citizen] as in [F1047_CreateCase],
    And a successful call [to confirm the same historical snapshot contains both fields for an authorised caseworker] as in [F1047_GetHistory_AsMaster],
    And a user with [an active profile in CCD],
    And a user with [case and event read access but no case-field read access],
    When a request is prepared with appropriate values,
    And the request [contains the reference of the case just created],
    And it is submitted to call the [Retrieve audit events by case ID] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains the event with an empty historical snapshot],
    And the response has all other details as expected.

  @S-1047.4
  Scenario: Unknown historical children are pruned with and without complex ACL overrides
    Given a successful call [to start decentralised case creation] as in [F1047_Nested_CreateCase_Token],
    And a successful call [to create a case owned by the nested-history reader] as in [F1047_Nested_CreateCase],
    And a successful call [to confirm the upstream snapshot contains unknown complex and collection children] as in [F1047_Nested_Upstream_History],
    And a user with [an active profile in CCD],
    And a user with [read access to the defined nested fields],
    When a request is prepared with appropriate values,
    And the request [contains the reference of the case just created],
    And it is submitted to call the [Retrieve audit events by case ID] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [omits unknown historical children and preserves readable siblings and collection IDs],
    And the response has all other details as expected.
