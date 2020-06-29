package uk.gov.hmcts.ccd.v2;

@SuppressWarnings("checkstyle:OperatorWrap") // too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
public class CaseRolesTestData {

    private CaseRolesTestData() {
        // Hide Utility Class Constructor : Utility classes should not have a public or default constructor
    }

    public static String getTestDefinition(final Integer portNumber) {

        final String port = portNumber.toString();

        return
            "{\n" +
                "  \"id\": \"CallbackCase\",\n" +
                "  \"version\": {\n" +
                "    \"number\": 1,\n" +
                "    \"live_from\": \"2017-01-01\"\n" +
                "  },\n" +
                "  \"name\": \"Callback Case\",\n" +
                "  \"description\": \"Test Callback Case\",\n" +
                "  \"jurisdiction\": {\n" +
                "    \"id\": \"TEST\",\n" +
                "    \"name\": \"Test\",\n" +
                "    \"description\": \"Test Jurisdiction\"\n" +
                "  },\n" +
                "  \"security_classification\": \"PUBLIC\",\n" +
                "  \"acls\": [\n" +
                "    {\n" +
                "        \"role\": \"caseworker-probate-public\",\n" +
                "        \"create\": true,\n" +
                "        \"read\": true,\n" +
                "        \"update\": true,\n" +
                "        \"delete\": false\n" +
                "   }]," +
                "  \"events\": [\n" +
                "    {\n" +
                "      \"id\": \"UPDATE-EVENT\",\n" +
                "      \"name\": \"Non Blocking\",\n" +
                "      \"description\": \"Test event for non null pre-states\",\n" +
                "      \"case_fields\": [\n" +
                "         {" +
                "            \"display_context\": \"OPTIONAL\",\n" +
                "            \"case_field_id\": \"PersonFirstName\"\n" +
                "         }," +
                "         {" +
                "            \"display_context\": \"OPTIONAL\",\n" +
                "            \"case_field_id\": \"PersonLastName\"\n" +
                "         }," +
                "         {" +
                "            \"display_context\": \"OPTIONAL\",\n" +
                "            \"case_field_id\": \"PersonAddress\"\n" +
                "         }" +
                "      ],\n" +
                "      \"pre_states\": [\n" +
                "        \"CaseCreated\"\n" +
                "      ],\n" +
                "      \"post_state\": \"CaseUpdated\",\n" +
                "      \"security_classification\": \"PUBLIC\",\n" +
                "      \"acls\": [\n" +
                "        {\n" +
                "            \"role\": \"caseworker-probate-public\",\n" +
                "            \"create\": true,\n" +
                "            \"read\": false,\n" +
                "            \"update\": false,\n" +
                "            \"delete\": false\n" +
                "       }]," +
                "      \"callback_url_about_to_start_event\" : \"http://localhost:" + port + "/before-start\",\n" +
                "      \"callback_url_about_to_submit_event\" : \"http://localhost:" + port + "/before-commit\",\n" +
                "      \"callback_url_submitted_event\" : \"http://localhost:" + port + "/after-commit\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"UPDATE-EVENT-NON-MATCHING\",\n" +
                "      \"name\": \"Non Matching pre states\",\n" +
                "      \"description\": \"Test event for non matching pre-states\",\n" +
                "      \"case_fields\": [\n" +
                "         {" +
                "            \"display_context\": \"OPTIONAL\",\n" +
                "            \"case_field_id\": \"PersonFirstName\"\n" +
                "         }," +
                "         {" +
                "            \"display_context\": \"OPTIONAL\",\n" +
                "            \"case_field_id\": \"PersonLastName\"\n" +
                "         }," +
                "         {" +
                "            \"display_context\": \"OPTIONAL\",\n" +
                "            \"case_field_id\": \"PersonAddress\"\n" +
                "         }" +
                "      ],\n" +
                "      \"pre_states\": [\n" +
                "        \"OtherState\",\n" +
                "        \"OtherStateTwo\"\n" +
                "      ],\n" +
                "      \"post_state\": \"CaseUpdated\",\n" +
                "      \"security_classification\": \"PUBLIC\",\n" +
                "      \"acls\": [\n" +
                "        {\n" +
                "            \"role\": \"caseworker-probate-public\",\n" +
                "            \"create\": true,\n" +
                "            \"read\": false,\n" +
                "            \"update\": false,\n" +
                "            \"delete\": false\n" +
                "       }]," +
                "      \"callback_url_about_to_start_event\" : \"http://localhost:" + port + "/before-start\",\n" +
                "      \"callback_url_about_to_submit_event\" : \"http://localhost:" + port + "/before-commit\",\n" +
                "      \"callback_url_submitted_event\" : \"http://localhost:" + port + "/after-commit\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"CREATE-CASE\",\n" +
                "      \"name\": \"Create Case\",\n" +
                "      \"description\": \"Just a test\",\n" +
                "      \"case_fields\": [\n" +
                "         {" +
                "            \"display_context\": \"OPTIONAL\",\n" +
                "            \"case_field_id\": \"PersonFirstName\"\n" +
                "         }," +
                "         {" +
                "            \"display_context\": \"OPTIONAL\",\n" +
                "            \"case_field_id\": \"PersonLastName\"\n" +
                "         }," +
                "         {" +
                "            \"display_context\": \"OPTIONAL\",\n" +
                "            \"case_field_id\": \"PersonAddress\"\n" +
                "         }" +
                "      ],\n" +
                "      \"pre_states\": [\n" +
                "      ],\n" +
                "      \"post_state\": \"CaseCreated\",\n" +
                "      \"security_classification\": \"PUBLIC\",\n" +
                "      \"acls\": [\n" +
                "        {\n" +
                "            \"role\": \"caseworker-probate-public\",\n" +
                "            \"create\": true,\n" +
                "            \"read\": true,\n" +
                "            \"update\": false,\n" +
                "            \"delete\": false\n" +
                "       }]," +
                "      \"callback_url_about_to_start_event\" : \"http://localhost:" + port + "/before-start\",\n" +
                "      \"callback_url_about_to_submit_event\" : \"http://localhost:" + port + "/before-commit\",\n" +
                "      \"callback_url_submitted_event\" : \"http://localhost:" + port + "/after-commit\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"PRE-STATES-NULL\",\n" +
                "      \"name\": \"Pre states null\",\n" +
                "      \"description\": \"Just a test\",\n" +
                "      \"case_fields\": [\n" +
                "         {" +
                "            \"display_context\": \"OPTIONAL\",\n" +
                "            \"case_field_id\": \"PersonFirstName\"\n" +
                "         }," +
                "         {" +
                "            \"display_context\": \"OPTIONAL\",\n" +
                "            \"case_field_id\": \"PersonLastName\"\n" +
                "         }," +
                "         {" +
                "            \"display_context\": \"OPTIONAL\",\n" +
                "            \"case_field_id\": \"PersonAddress\"\n" +
                "         }" +
                "      ],\n" +
                "      \"pre_states\": null,\n" +
                "      \"post_state\": \"CaseCreated\",\n" +
                "      \"security_classification\": \"PUBLIC\",\n" +
                "      \"acls\": [\n" +
                "        {\n" +
                "            \"role\": \"caseworker-probate-public\",\n" +
                "            \"create\": true,\n" +
                "            \"read\": false,\n" +
                "            \"update\": false,\n" +
                "            \"delete\": false\n" +
                "       }]," +
                "      \"callback_url_about_to_start_event\" : \"http://localhost:" + port + "/before-start\",\n" +
                "      \"callback_url_about_to_submit_event\" : \"http://localhost:" + port + "/before-commit\",\n" +
                "      \"callback_url_submitted_event\" : \"http://localhost:" + port + "/after-commit\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"states\": [\n" +
                "    {\n" +
                "      \"id\": \"CaseCreated\",\n" +
                "      \"name\": \"Case Created\",\n" +
                "      \"acls\": [\n" +
                "        {\n" +
                "            \"role\": \"caseworker-probate-public\",\n" +
                "            \"create\": false,\n" +
                "            \"read\": true,\n" +
                "            \"update\": true,\n" +
                "            \"delete\": false\n" +
                "       }]" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"CaseUpdated\",\n" +
                "      \"name\": \"Case Updated\",\n" +
                "      \"acls\": [\n" +
                "        {\n" +
                "            \"role\": \"caseworker-probate-public\",\n" +
                "            \"create\": false,\n" +
                "            \"read\": true,\n" +
                "            \"update\": true,\n" +
                "            \"delete\": false\n" +
                "       }]" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"CaseEnteredIntoLegacy\",\n" +
                "      \"name\": \"Case Has Been Entered Into Legacy\",\n" +
                "      \"acls\": [\n" +
                "        {\n" +
                "            \"role\": \"caseworker-probate-public\",\n" +
                "            \"create\": false,\n" +
                "            \"read\": true,\n" +
                "            \"update\": true,\n" +
                "            \"delete\": false\n" +
                "       }]" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"CaseStopped\",\n" +
                "      \"name\": \"Put case on hold\",\n" +
                "      \"acls\": [\n" +
                "        {\n" +
                "            \"role\": \"caseworker-probate-public\",\n" +
                "            \"create\": false,\n" +
                "            \"read\": true,\n" +
                "            \"update\": true,\n" +
                "            \"delete\": false\n" +
                "       }]" +
                "    }\n" +
                "  ],\n" +
                "  \"case_fields\": [\n" +
                "    {\n" +
                "      \"id\": \"PersonFirstName\",\n" +
                "      \"case_type_id\": \"TestAddressBookCase\",\n" +
                "      \"label\": \"First name\",\n" +
                "      \"security_classification\": \"PUBLIC\",\n" +
                "      \"acls\": [\n" +
                "        {\n" +
                "          \"role\": \"caseworker-probate-public\",\n" +
                "          \"create\": false,\n" +
                "          \"read\": false,\n" +
                "          \"update\": false,\n" +
                "          \"delete\": false\n" +
                "        },\n" +
                "        {\n" +
                "          \"role\": \"[CREATOR]\",\n" +
                "          \"create\": true,\n" +
                "          \"read\": true,\n" +
                "          \"update\": true,\n" +
                "          \"delete\": false\n" +
                "       }]," +
                "      \"field_type\": {\n" +
                "        \"type\": \"Text\",\n" +
                "        \"id\": \"Text\",\n" +
                "        \"regular_expression\": \"ccd-.*\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"PersonLastName\",\n" +
                "      \"case_type_id\": \"TestAddressBookCase\",\n" +
                "      \"label\": \"Last name\",\n" +
                "      \"security_classification\": \"PUBLIC\",\n" +
                "      \"acls\": [\n" +
                "        {\n" +
                "            \"role\": \"caseworker-probate-public\",\n" +
                "            \"create\": true,\n" +
                "            \"read\": true,\n" +
                "            \"update\": true,\n" +
                "            \"delete\": false\n" +
                "       }]," +
                "      \"field_type\": {\n" +
                "        \"type\": \"Text\",\n" +
                "        \"id\": \"Text\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"PersonAddress\",\n" +
                "      \"case_type_id\": \"TestAddressBookCase\",\n" +
                "      \"label\": \"Address\",\n" +
                "      \"security_classification\": \"PUBLIC\",\n" +
                "      \"acls\": [\n" +
                "        {\n" +
                "            \"role\": \"caseworker-probate-public\",\n" +
                "            \"create\": true,\n" +
                "            \"read\": true,\n" +
                "            \"update\": true,\n" +
                "            \"delete\": false\n" +
                "       }]," +
                "      \"field_type\": {\n" +
                "        \"id\": \"Address\",\n" +
                "        \"type\": \"Complex\",\n" +
                "        \"complex_fields\": [\n" +
                "          {\n" +
                "            \"id\": \"Country\",\n" +
                "            \"security_classification\": \"PUBLIC\",\n" +
                "            \"field_type\": {\n" +
                "              \"id\": \"Text\",\n" +
                "              \"type\": \"Text\"\n" +
                "            }\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\": \"Postcode\",\n" +
                "            \"security_classification\": \"PUBLIC\",\n" +
                "            \"field_type\": {\n" +
                "              \"id\": \"Text\",\n" +
                "              \"type\": \"Text\"\n" +
                "            }\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\": \"AddressLine1\",\n" +
                "            \"security_classification\": \"PUBLIC\",\n" +
                "            \"label\": \"AddressLine1\",\n" +
                "            \"field_type\": {\n" +
                "              \"id\": \"Text\",\n" +
                "              \"type\": \"Text\"\n" +
                "            }\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\": \"AddressLine2\",\n" +
                "            \"security_classification\": \"PUBLIC\",\n" +
                "            \"field_type\": {\n" +
                "              \"id\": \"Text\",\n" +
                "              \"type\": \"Text\"\n" +
                "            }\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\": \"AddressLine3\",\n" +
                "            \"security_classification\": \"PUBLIC\",\n" +
                "            \"field_type\": {\n" +
                "              \"id\": \"Text\",\n" +
                "              \"type\": \"Text\"\n" +
                "            }\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    }," +
                "    {\n" +
                "      \"id\": \"D8Document\",\n" +
                "      \"case_type_id\": \"TestAddressBookCase\",\n" +
                "      \"label\": \"Document\",\n" +
                "      \"security_classification\": \"PUBLIC\",\n" +
                "      \"acls\": [\n" +
                "        {\n" +
                "            \"role\": \"caseworker-probate-public\",\n" +
                "            \"create\": true,\n" +
                "            \"read\": true,\n" +
                "            \"update\": true,\n" +
                "            \"delete\": false\n" +
                "       }]," +
                "      \"field_type\": {\n" +
                "        \"type\": \"Document\",\n" +
                "        \"id\": \"Document\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

    }
}
