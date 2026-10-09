#=========================================================================================================================================================
# CCD-2008: Postcode field type must accept valid UK postcodes (e.g. W1U 3BW) and reject invalid ones.
#
# End-to-end value validation for the Postcode base-type regex fix (def-store PR#1762). The def-store
# fix only changes the regex *string*; this feature proves data-store actually accepts/rejects postcode
# *values* against it when a case is created with a Postcode field.
#
# @Ignore until BOTH preconditions are met in the data-store preview environment:
#   1. ccd-test-definitions branch ccd-2008-postcode-field is released and picked up by data-store
#      (adds the PostcodeField of FieldType Postcode to FT_Regex - without it there is no field to set).
#   2. ccd-definition-store-api PR#1762 (the corrected Postcode regex) is released, so the deployed
#      def-store serves the new regex. Until then the live regex still rejects W1U 3BW and S-1008.1 fails.
# When both are live, remove the @Ignore tags. On first enable, confirm the S-1008.1 expected case_data
# shape (FT_Regex create response) and the S-1008.2 422 body shape match the deployed responses.
#=========================================================================================================
@F-1008
Feature: F-1008: Postcode field type accepts valid UK postcodes and rejects invalid ones

  Background:
    Given an appropriate test context as detailed in the test data source

  #-------------------------------------------------------------------------------------------------------
  @S-1008.1 @Ignore # CCD-2008: enable once def-store PR#1762 regex + ccd-test-definitions PostcodeField are released to data-store preview
  Scenario: A case created with a valid UK postcode (W1U 3BW) in a Postcode field is accepted

    Given a user with [an active caseworker profile in CCD with full permissions on the postcode field],
      And a successful call [to create a token for case creation] as in [F-1008_Postcode_Token_Creation],

     When a request is prepared with appropriate values,
      And the request [contains a valid UK postcode in the Postcode field],
      And it is submitted to call the [Submit Case Creation] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [is accepted and the postcode is persisted],
      And the response has all other details as expected.

  #-------------------------------------------------------------------------------------------------------
  @S-1008.2 @Ignore # CCD-2008: enable once def-store PR#1762 regex + ccd-test-definitions PostcodeField are released to data-store preview
  Scenario: A case created with an invalid postcode in a Postcode field is rejected

    Given a user with [an active caseworker profile in CCD with full permissions on the postcode field],
      And a successful call [to create a token for case creation] as in [F-1008_Postcode_Token_Creation],

     When a request is prepared with appropriate values,
      And the request [contains a value that is not a valid UK postcode in the Postcode field],
      And it is submitted to call the [Submit Case Creation] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [is rejected with an unprocessable entity error],
      And the response has all other details as expected.
