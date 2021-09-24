Feature:

  Scenario: Successfully allow the creation of a case payment order in the Case Payment Order database


  When a request is prepared with appropriate values

  And the request intends to [Create a case payment order]

  And the request contains [all the mandatory parameters]

  And it is submitted to call the [API operation [createPaymentOrder] of the Create Payment Order API]

  Then a positive response is received

  And the response contains [a 200 success OK code]

  And the response contains [a valid UUID of the case order record]

  And the response has all other details as expected

  And a call [to verify that a Case payment Order has been created in the database which can be accessed or queried again at a later date] will get the expected response as in [YYY].

  And a call [to verify that the the request, correlation_id And user_id together with success status have been logged] will get the expected response as in [XXX].



  Scenario: AC2- Must return error if one or more of the mandatory parameters have not been provided (Please refer to the mandatory parameter list in the description)

  Given a [a new ”Case-Order” microservice has been established]

  When a request is prepared with appropriate values

  And the request intends to [Create a case payment order]

  And the request [does not contain one or more of the mandatory parameters]

  And it is submitted to call the [API operation [createPaymentOrder] of the Create Payment Order API]

  Then a negative response is received

  And the response has all other details as expected

  And a call [to verify that a Case payment Order has not been created in the database] will get the expected response as in [YYY].



  Scenario: AC3 - Must return an error if the request contains an invalid mandatory parameter

  Given a [a new ”Case-Order” microservice has been established]

  When a request is prepared with appropriate values

  And the request intends to [Create a case payment order]

  And the request contains [an invalid mandatory parameter]

  And it is submitted to call the [API operation [createPaymentOrder] of the Create Payment Order API]

  Then a negative response is received

  And the response has all other details as expected

  And a call [to verify that a Case payment Order has not been created in the database which can be accessed or queried again at a later date] will get the expected response as in [YYY].



  Scenario: AC4- Must return error if order_reference/case_id is non-unique (Case order record already exists in the database for the same order reference)

  Given a [a new ”Case-Order” microservice has been established]

  When a request is prepared with appropriate values

  And the request intends to [Create a case payment order]

  And the request contains [all the mandatory parameters]

  And the request contains [an order_reference/case_id which is non-unique (Case order record already exists in the database for the same order reference)]

  And it is submitted to call the [API operation [createPaymentOrder] of the Create Payment Order API]

  Then a negative response is received

  And the response has all other details as expected

  And a call [to verify that a Case payment Order has not been created in the database which can be accessed or queried again at a later date] will get the expected response as in [YYY].
