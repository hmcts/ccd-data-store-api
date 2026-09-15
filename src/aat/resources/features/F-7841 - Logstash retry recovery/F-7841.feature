@F-7841 @elasticsearch @logstash-outage
Feature: F-7841: Logstash reclaims a queued case update after Elasticsearch recovers

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source
    And a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of [5] seconds [to allow for the initial case to be indexed]

  @S-7841.1
  Scenario: supplementary data is indexed after an Elasticsearch write outage
    Given Elasticsearch writes are blocked for the AAT private-case index
    And a successful call [to add supplementary data for the case] as in [F-120_Add_Supplementary_Data]
    And a wait time of [10] seconds [to allow Logstash to attempt the failed Elasticsearch write]
    And Elasticsearch writes are restored for the AAT private-case index
    And a wait time of [315] seconds [to allow the five-minute claim lease to expire and Logstash to reclaim and index the queued update]
    And a user with [a valid profile]
    When a request is prepared with appropriate values
    And it is submitted to call the [External Elastic Search Endpoint] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response has all other details as expected
