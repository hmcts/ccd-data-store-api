package uk.gov.hmcts.ccd.domain.service.caselinking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinkDetails;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinkInfo;
import uk.gov.hmcts.ccd.domain.model.caselinking.GetLinkedCasesResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkExtractor.STANDARD_CASE_LINK_FIELD;
import static uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkTestFixtures.createCaseLinkCollectionString;

class GetLinkedCasesResponseCreatorTest {

    private List<CaseDetails> caseDetails;

    private GetLinkedCasesResponseCreator getLinkedCasesResponseCreator;

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    private static final String CASE_REFERENCE = "1648478073517926";

    private static final String CASE_DETAILS_TEMPLATE = "{\n"
        + "  \"id\": %s,\n"
        + "  \"jurisdiction\": \"%s\",\n"
        + "  \"state\": \"%s\",\n"
        + "  \"version\": 1,\n"
        + "  \"case_type_id\": \"%s\",\n"
        + "  \"created_date\": \"2016-06-22T20:44:52.824\",\n"
        + "  \"last_modified\": \"2016-06-24T20:44:52.824\",\n"
        + "  \"last_state_modified_date\": \"2016-06-24T20:44:52.824\",\n"
        + "  \"security_classification\": \"PUBLIC\",\n"
        + "  \"case_data\": {\n"
        + "    \"caseNameHmctsInternal\" : \"%s\",\n"
        + "    \"caseLinks\" : [ {\n"
        // many reasons
        + "      \"id\" : \"8d64133f-cde0-4db7-bdbe-6cb767c63d7d\",\n"
        + "      \"value\" : {\n"
        + "        \"CaseReference\" : \"" + CASE_REFERENCE + "\",\n"
        + "        \"CaseType\" : \"MyBaseType\",\n"
        + "        \"CreatedDateTime\" : \"2022-04-14T01:46:57.947877\",\n"
        + "        \"ReasonForLink\" : [ {\n"
        + "          \"id\" : \"57bc2066-545e-4020-8365-5cf4512b3c85\",\n"
        + "          \"value\" : {\n"
        + "            \"Reason\" : \"Reason 1.1\",\n"
        + "            \"OtherDescription\" : \"OtherDescription 1.1\"\n"
        + "          }\n"
        + "        }, {\n"
        + "          \"id\" : \"2f069606-18ca-453a-893f-a32c31443b16\",\n"
        + "          \"value\" : {\n"
        + "            \"Reason\" : \"Reason 1.2\",\n"
        + "            \"OtherDescription\" : \"OtherDescription 1.2\"\n"
        + "         }\n"
        + "        } ]\n"
        + "      }\n"
        + "    }, {\n"
        // to be ignored as wrong case reference
        + "      \"id\" : \"d0eec7af-4bf0-4a24-9676-1d2d4dc736e6\",\n"
        + "      \"value\" : {\n"
        + "        \"CaseReference\" : \"4444333322221111\",\n"
        + "        \"CaseType\" : \"MyBaseType\",\n"
        + "        \"CreatedDateTime\" : \"2022-03-24T09:08:15.947877\",\n"
        + "        \"ReasonForLink\" : [ {\n"
        + "          \"id\" : \"b38a2996-3ddb-42fa-85d5-c8b07387e1ae\",\n"
        + "          \"value\" : {\n"
        + "            \"Reason\" : \"Reason and link ignored in test (wrong case reference)\",\n"
        + "            \"OtherDescription\" : \"OtherDescription\"\n"
        + "          }\n"
        + "        } ]\n"
        + "      }\n"
        + "    }, {\n"
        // single reason
        + "      \"id\" : \"ddd50637-1e17-4395-a101-e65b3ed4e634\",\n"
        + "      \"value\" : {\n"
        + "        \"CaseReference\" : \"" + CASE_REFERENCE + "\",\n"
        + "        \"CaseType\" : \"MyBaseType\",\n"
        + "        \"CreatedDateTime\" : \"2022-03-24T09:08:15.947877\",\n"
        + "        \"ReasonForLink\" : [ {\n"
        + "          \"id\" : \"02d7b1a5-d5b7-4abd-8991-59ab8c1b4136\",\n"
        + "          \"value\" : {\n"
        + "            \"Reason\" : \"Reason 2.1\",\n"
        + "            \"OtherDescription\" : \"OtherDescription 2.1\"\n"
        + "          }\n"
        + "        } ]\n"
        + "      }\n"
        + "    }, {\n"
        // minimal case link (i.e. no reasons or date time)
        + "      \"id\" : \"f113b206-9ebd-4e8e-b1c6-ee0093167e1a\",\n"
        + "      \"value\" : {\n"
        + "        \"CaseReference\" : \"" + CASE_REFERENCE + "\"\n"
        + "      }\n"
        + "    } ],\n"
        + "    \"PersonAddress\": {\n"
        + "      \"Country\": \"England\",\n"
        + "      \"Postcode\": \"HX08 5TG\",\n"
        + "      \"AddressLine1\": \"123\",\n"
        + "      \"AddressLine2\": \"Fake Street\",\n"
        + "      \"AddressLine3\": \"Hexton\"\n"
        + "    },\n"
        + "    \"PersonLastName\": \"Parker\",\n"
        + "    \"PersonFirstName\": \"Janet\"\n"
        + "  },\n"
        + "  \"data_classification\": {\n"
        + "   \"caseNameHmctsInternal\" : \"PUBLIC\",\n"
        + "    \"PersonAddress\": {\n"
        + "      \"value\": {\n"
        + "        \"Country\": \"PUBLIC\",\n"
        + "        \"Postcode\": \"PUBLIC\",\n"
        + "        \"AddressLine1\": \"PUBLIC\",\n"
        + "        \"AddressLine2\": \"PUBLIC\",\n"
        + "        \"AddressLine3\": \"PUBLIC\"\n"
        + "      },\n"
        + "      \"classification\": \"PUBLIC\"\n"
        + "    },\n"
        + "    \"PersonLastName\": \"PUBLIC\",\n"
        + "    \"PersonFirstName\": \"PUBLIC\"\n"
        + "  },\n"
        + "  \"supplementary_data\": null,\n"
        + "  \"after_submit_callback_response\": null,\n"
        + "  \"callback_response_status_code\": null,\n"
        + "  \"callback_response_status\": null,\n"
        + "  \"delete_draft_response_status_code\": null,\n"
        + "  \"delete_draft_response_status\": null\n"
        + "}";

    private static final String CASE_DETAILS_TEMPLATE_NO_CASE_LINK_FIELDS = "{\n"
        + "  \"id\": %s,\n"
        + "  \"jurisdiction\": \"%s\",\n"
        + "  \"state\": \"%s\",\n"
        + "  \"version\": 1,\n"
        + "  \"case_type_id\": \"%s\",\n"
        + "  \"created_date\": \"2016-06-22T20:44:52.824\",\n"
        + "  \"last_modified\": \"2016-06-24T20:44:52.824\",\n"
        + "  \"last_state_modified_date\": \"2016-06-24T20:44:52.824\",\n"
        + "  \"security_classification\": \"PUBLIC\",\n"
        + "  \"case_data\": {\n"
        + "    \"caseNameHmctsInternal\" : \"%s\",\n"
        + "  \"PersonAddress\": {\n"
        + "    \"Country\": \"England\",\n"
        + "    \"Postcode\": \"HX08 5TG\",\n"
        + "    \"AddressLine1\": \"123\",\n"
        + "    \"AddressLine2\": \"Fake Street\",\n"
        + "    \"AddressLine3\": \"Hexton\"\n"
        + "  },\n"
        + "    \"PersonLastName\": \"Parker\",\n"
        + "    \"PersonFirstName\": \"Janet\"\n"
        + "  },\n"
        + "  \"data_classification\": {\n"
        + "   \"caseNameHmctsInternal\" : \"PUBLIC\",\n"
        + "    \"PersonAddress\": {\n"
        + "      \"value\": {\n"
        + "        \"Country\": \"PUBLIC\",\n"
        + "        \"Postcode\": \"PUBLIC\",\n"
        + "        \"AddressLine1\": \"PUBLIC\",\n"
        + "        \"AddressLine2\": \"PUBLIC\",\n"
        + "        \"AddressLine3\": \"PUBLIC\"\n"
        + "      },\n"
        + "      \"classification\": \"PUBLIC\"\n"
        + "    },\n"
        + "    \"PersonLastName\": \"PUBLIC\",\n"
        + "    \"PersonFirstName\": \"PUBLIC\"\n"
        + "  },\n"
        + "  \"supplementary_data\": null,\n"
        + "  \"after_submit_callback_response\": null,\n"
        + "  \"callback_response_status_code\": null,\n"
        + "  \"callback_response_status\": null,\n"
        + "  \"delete_draft_response_status_code\": null,\n"
        + "  \"delete_draft_response_status\": null\n"
        + "}";

    private static final String CASE_DETAILS_TEMPLATE_NO_LINK_REASON_CASE_LINK_FIELDS = "{\n"
        + "  \"id\": %s,\n"
        + "  \"jurisdiction\": \"%s\",\n"
        + "  \"state\": \"%s\",\n"
        + "  \"version\": 1,\n"
        + "  \"case_type_id\": \"%s\",\n"
        + "  \"created_date\": \"2016-06-22T20:44:52.824\",\n"
        + "  \"last_modified\": \"2016-06-24T20:44:52.824\",\n"
        + "  \"last_state_modified_date\": \"2016-06-24T20:44:52.824\",\n"
        + "  \"security_classification\": \"PUBLIC\",\n"
        + "  \"case_data\": {\n"
        + "    \"caseNameHmctsInternal\" : \"%s\",\n"
        + "    \"caseLinks\" : [ {\n"
        // many reasons
        + "      \"id\" : \"8d64133f-cde0-4db7-bdbe-6cb767c63d7d\",\n"
        + "      \"value\" : {\n"
        + "        \"CaseReference\" : \"" + CASE_REFERENCE + "\",\n"
        + "        \"CaseType\" : \"MyBaseType\",\n"
        + "        \"CreatedDateTime\" : \"2022-04-14T01:46:57.947877\"\n"
        //+ "        \"CreatedDateTime\" : \"2022-04-14T01:46:57.947877\",\n"
        //+ "        \"ReasonForLink\" : [ {\n"
        //+ "          \"id\" : \"57bc2066-545e-4020-8365-5cf4512b3c85\",\n"
        //+ "          \"value\" : {\n"
        //+ "            \"Reason\" : \"Reason 1.1\",\n"
        //+ "            \"OtherDescription\" : \"OtherDescription 1.1\"\n"
        //+ "          }\n"
        //+ "        }, {\n"
        //+ "          \"id\" : \"2f069606-18ca-453a-893f-a32c31443b16\",\n"
        //+ "          \"value\" : {\n"
        //+ "            \"Reason\" : \"Reason 1.2\",\n"
        //+ "            \"OtherDescription\" : \"OtherDescription 1.2\"\n"
        //+ "         }\n"
        //+ "        } ]\n"
        + "      }\n"
        + "    }, {\n"
        // to be ignored as wrong case reference
        + "      \"id\" : \"d0eec7af-4bf0-4a24-9676-1d2d4dc736e6\",\n"
        + "      \"value\" : {\n"
        + "        \"CaseReference\" : \"4444333322221111\",\n"
        + "        \"CaseType\" : \"MyBaseType\",\n"
        + "        \"CreatedDateTime\" : \"2022-03-24T09:08:15.947877\"\n"
        /*+ "        \"CreatedDateTime\" : \"2022-03-24T09:08:15.947877\",\n"
        + "        \"ReasonForLink\" : [ {\n"
        + "          \"id\" : \"b38a2996-3ddb-42fa-85d5-c8b07387e1ae\",\n"
        + "          \"value\" : {\n"
        + "            \"Reason\" : \"Reason and link ignored in test (wrong case reference)\",\n"
        + "            \"OtherDescription\" : \"OtherDescription\"\n"
        + "          }\n"
        + "        } ]\n"*/
        + "      }\n"
        + "    }, {\n"
        // single reason
        + "      \"id\" : \"ddd50637-1e17-4395-a101-e65b3ed4e634\",\n"
        + "      \"value\" : {\n"
        + "        \"CaseReference\" : \"" + CASE_REFERENCE + "\",\n"
        + "        \"CaseType\" : \"MyBaseType\",\n"
        + "        \"CreatedDateTime\" : \"2022-03-24T09:08:15.947877\"\n"
        /*+ "        \"CreatedDateTime\" : \"2022-03-24T09:08:15.947877\",\n"
        + "        \"ReasonForLink\" : [ {\n"
        + "          \"id\" : \"02d7b1a5-d5b7-4abd-8991-59ab8c1b4136\",\n"
        + "          \"value\" : {\n"
        + "            \"Reason\" : \"Reason 2.1\",\n"
        + "            \"OtherDescription\" : \"OtherDescription 2.1\"\n"
        + "          }\n"
        + "        } ]\n"*/
        + "      }\n"
        + "    }, {\n"
        // minimal case link (i.e. no reasons or date time)
        + "      \"id\" : \"f113b206-9ebd-4e8e-b1c6-ee0093167e1a\",\n"
        + "      \"value\" : {\n"
        + "        \"CaseReference\" : \"" + CASE_REFERENCE + "\"\n"
        + "      }\n"
        + "    } ],\n"
        + "    \"PersonAddress\": {\n"
        + "      \"Country\": \"England\",\n"
        + "      \"Postcode\": \"HX08 5TG\",\n"
        + "      \"AddressLine1\": \"123\",\n"
        + "      \"AddressLine2\": \"Fake Street\",\n"
        + "      \"AddressLine3\": \"Hexton\"\n"
        + "    },\n"
        + "    \"PersonLastName\": \"Parker\",\n"
        + "    \"PersonFirstName\": \"Janet\"\n"
        + "  },\n"
        + "  \"data_classification\": {\n"
        + "   \"caseNameHmctsInternal\" : \"PUBLIC\",\n"
        + "    \"PersonAddress\": {\n"
        + "      \"value\": {\n"
        + "        \"Country\": \"PUBLIC\",\n"
        + "        \"Postcode\": \"PUBLIC\",\n"
        + "        \"AddressLine1\": \"PUBLIC\",\n"
        + "        \"AddressLine2\": \"PUBLIC\",\n"
        + "        \"AddressLine3\": \"PUBLIC\"\n"
        + "      },\n"
        + "      \"classification\": \"PUBLIC\"\n"
        + "    },\n"
        + "    \"PersonLastName\": \"PUBLIC\",\n"
        + "    \"PersonFirstName\": \"PUBLIC\"\n"
        + "  },\n"
        + "  \"supplementary_data\": null,\n"
        + "  \"after_submit_callback_response\": null,\n"
        + "  \"callback_response_status_code\": null,\n"
        + "  \"callback_response_status\": null,\n"
        + "  \"delete_draft_response_status_code\": null,\n"
        + "  \"delete_draft_response_status\": null\n"
        + "}";

    private static final String CASE_DETAILS_TEMPLATE_EMPTY_LINK_REASON_CASE_LINK_FIELDS = "{\n"
        + "  \"id\": %s,\n"
        + "  \"jurisdiction\": \"%s\",\n"
        + "  \"state\": \"%s\",\n"
        + "  \"version\": 1,\n"
        + "  \"case_type_id\": \"%s\",\n"
        + "  \"created_date\": \"2016-06-22T20:44:52.824\",\n"
        + "  \"last_modified\": \"2016-06-24T20:44:52.824\",\n"
        + "  \"last_state_modified_date\": \"2016-06-24T20:44:52.824\",\n"
        + "  \"security_classification\": \"PUBLIC\",\n"
        + "  \"case_data\": {\n"
        + "    \"caseNameHmctsInternal\" : \"%s\",\n"
        + "    \"caseLinks\" : [ {\n"
        // many reasons
        + "      \"id\" : \"8d64133f-cde0-4db7-bdbe-6cb767c63d7d\",\n"
        + "      \"value\" : {\n"
        + "        \"CaseReference\" : \"" + CASE_REFERENCE + "\",\n"
        + "        \"CaseType\" : \"MyBaseType\",\n"
        + "        \"CreatedDateTime\" : \"2022-04-14T01:46:57.947877\",\n"
        + "        \"ReasonForLink\" : [ {\n"
        //+ "          \"id\" : \"57bc2066-545e-4020-8365-5cf4512b3c85\",\n"
        //+ "          \"value\" : {\n"
        //+ "            \"Reason\" : \"Reason 1.1\",\n"
        //+ "            \"OtherDescription\" : \"OtherDescription 1.1\"\n"
        //+ "          }\n"
        //+ "        }, {\n"
        //+ "          \"id\" : \"2f069606-18ca-453a-893f-a32c31443b16\",\n"
        //+ "          \"value\" : {\n"
        //+ "            \"Reason\" : \"Reason 1.2\",\n"
        //+ "            \"OtherDescription\" : \"OtherDescription 1.2\"\n"
        //+ "         }\n"
        + "        } ]\n"
        + "      }\n"
        + "    }, {\n"
        // to be ignored as wrong case reference
        + "      \"id\" : \"d0eec7af-4bf0-4a24-9676-1d2d4dc736e6\",\n"
        + "      \"value\" : {\n"
        + "        \"CaseReference\" : \"4444333322221111\",\n"
        + "        \"CaseType\" : \"MyBaseType\",\n"
        + "        \"CreatedDateTime\" : \"2022-03-24T09:08:15.947877\"\n"
        /*+ "        \"CreatedDateTime\" : \"2022-03-24T09:08:15.947877\",\n"
        + "        \"ReasonForLink\" : [ {\n"
        + "          \"id\" : \"b38a2996-3ddb-42fa-85d5-c8b07387e1ae\",\n"
        + "          \"value\" : {\n"
        + "            \"Reason\" : \"Reason and link ignored in test (wrong case reference)\",\n"
        + "            \"OtherDescription\" : \"OtherDescription\"\n"
        + "          }\n"
        + "        } ]\n"*/
        + "      }\n"
        + "    }, {\n"
        // single reason
        + "      \"id\" : \"ddd50637-1e17-4395-a101-e65b3ed4e634\",\n"
        + "      \"value\" : {\n"
        + "        \"CaseReference\" : \"" + CASE_REFERENCE + "\",\n"
        + "        \"CaseType\" : \"MyBaseType\",\n"
        + "        \"CreatedDateTime\" : \"2022-03-24T09:08:15.947877\"\n"
        /*+ "        \"CreatedDateTime\" : \"2022-03-24T09:08:15.947877\",\n"
        + "        \"ReasonForLink\" : [ {\n"
        + "          \"id\" : \"02d7b1a5-d5b7-4abd-8991-59ab8c1b4136\",\n"
        + "          \"value\" : {\n"
        + "            \"Reason\" : \"Reason 2.1\",\n"
        + "            \"OtherDescription\" : \"OtherDescription 2.1\"\n"
        + "          }\n"
        + "        } ]\n"*/
        + "      }\n"
        + "    }, {\n"
        // minimal case link (i.e. no reasons or date time)
        + "      \"id\" : \"f113b206-9ebd-4e8e-b1c6-ee0093167e1a\",\n"
        + "      \"value\" : {\n"
        + "        \"CaseReference\" : \"" + CASE_REFERENCE + "\"\n"
        + "      }\n"
        + "    } ],\n"
        + "    \"PersonAddress\": {\n"
        + "      \"Country\": \"England\",\n"
        + "      \"Postcode\": \"HX08 5TG\",\n"
        + "      \"AddressLine1\": \"123\",\n"
        + "      \"AddressLine2\": \"Fake Street\",\n"
        + "      \"AddressLine3\": \"Hexton\"\n"
        + "    },\n"
        + "    \"PersonLastName\": \"Parker\",\n"
        + "    \"PersonFirstName\": \"Janet\"\n"
        + "  },\n"
        + "  \"data_classification\": {\n"
        + "   \"caseNameHmctsInternal\" : \"PUBLIC\",\n"
        + "    \"PersonAddress\": {\n"
        + "      \"value\": {\n"
        + "        \"Country\": \"PUBLIC\",\n"
        + "        \"Postcode\": \"PUBLIC\",\n"
        + "        \"AddressLine1\": \"PUBLIC\",\n"
        + "        \"AddressLine2\": \"PUBLIC\",\n"
        + "        \"AddressLine3\": \"PUBLIC\"\n"
        + "      },\n"
        + "      \"classification\": \"PUBLIC\"\n"
        + "    },\n"
        + "    \"PersonLastName\": \"PUBLIC\",\n"
        + "    \"PersonFirstName\": \"PUBLIC\"\n"
        + "  },\n"
        + "  \"supplementary_data\": null,\n"
        + "  \"after_submit_callback_response\": null,\n"
        + "  \"callback_response_status_code\": null,\n"
        + "  \"callback_response_status\": null,\n"
        + "  \"delete_draft_response_status_code\": null,\n"
        + "  \"delete_draft_response_status\": null\n"
        + "}";

    @BeforeEach
    void setup() {
        caseDetails = new ArrayList<>();
        getLinkedCasesResponseCreator = new GetLinkedCasesResponseCreator();
    }

    private CaseDetails createCaseDetail(List<String> parameters) throws JsonProcessingException {
        if (parameters.size() != 5) {
            fail("Need 5 strings to create a CaseDetails");
        }

        final String caseDetails = String.format(CASE_DETAILS_TEMPLATE, parameters.get(0), parameters.get(1),
            parameters.get(2), parameters.get(3), parameters.get(4));
        return OBJECT_MAPPER.readValue(caseDetails, CaseDetails.class);
    }

    @Test
    void testCreateCaseLinkInfoList() throws JsonProcessingException {
        List<String> caseDetails1 = List.of("1500638105106660",
            "jurisdiction",
            "state",
            "caseTypeId",
            "caseNameHmctsInternal");
        List<String> caseDetails2 = List.of("9514840069336542",
            "jurisdiction2",
            "state2",
            "caseTypeId2",
            "caseNameHmctsInternal2");
        List<String> caseDetails3 = List.of("4827897342988773",
            "jurisdiction3",
            "state3",
            "caseTypeId3",
            "caseNameHmctsInternal3");
        caseDetails = List.of(createCaseDetail(caseDetails1),
            createCaseDetail(caseDetails2),
            createCaseDetail(caseDetails3));

        CaseLinkRetrievalResults caseLinkRetrievalResults = CaseLinkRetrievalResults.builder()
            .caseDetails(caseDetails)
            .hasMoreResults(false)
            .build();

        final GetLinkedCasesResponse response = getLinkedCasesResponseCreator.createResponse(caseLinkRetrievalResults,
            CASE_REFERENCE);

        final List<CaseLinkInfo> caseLinks = response.getLinkedCases();

        assertEquals(caseDetails.size(), caseLinks.size());

        assertCaseLinkInfo(caseLinks.get(0), caseDetails1);
        assertCaseLinkInfo(caseLinks.get(1), caseDetails2);
        assertCaseLinkInfo(caseLinks.get(2), caseDetails3);

        assertFalse(response.isHasMoreRecords());
    }

    @Test
    void testCreateCaseLinkInfoListNoCaseLinkFieldsPresent() throws JsonProcessingException {
        final String caseDetails = String.format(CASE_DETAILS_TEMPLATE_NO_CASE_LINK_FIELDS, "1500638105106660",
            "jurisdiction", "state", "caseTypeId", "caseNameHmctsInternal");

        CaseLinkRetrievalResults caseLinkRetrievalResults = CaseLinkRetrievalResults.builder()
            .caseDetails(List.of(OBJECT_MAPPER.readValue(caseDetails, CaseDetails.class)))
            .hasMoreResults(false)
            .build();

        final GetLinkedCasesResponse response = getLinkedCasesResponseCreator.createResponse(caseLinkRetrievalResults,
            CASE_REFERENCE);

        final List<CaseLinkInfo> caseLinks = response.getLinkedCases();

        assertTrue(caseLinks.get(0).getLinkDetails().isEmpty());
        assertFalse(response.isHasMoreRecords());
    }

    @Test
    void testCreateCaseLinkInfoListNoLinkReasonCaseLinkFieldsPresent() throws JsonProcessingException {
        final String caseDetails =
            String.format(CASE_DETAILS_TEMPLATE_NO_LINK_REASON_CASE_LINK_FIELDS, "1500638105106660",
            "jurisdiction", "state", "caseTypeId", "caseNameHmctsInternal");

        CaseLinkRetrievalResults caseLinkRetrievalResults = CaseLinkRetrievalResults.builder()
            .caseDetails(List.of(OBJECT_MAPPER.readValue(caseDetails, CaseDetails.class)))
            .hasMoreResults(false)
            .build();

        final GetLinkedCasesResponse response = getLinkedCasesResponseCreator.createResponse(caseLinkRetrievalResults,
            CASE_REFERENCE);

        final List<CaseLinkInfo> caseLinks = response.getLinkedCases();

        assertFalse(response.getLinkedCases().isEmpty());
        assertEquals(caseLinks.size(), 1);
        assertEquals(caseLinks.get(0).getLinkDetails().size(), 3);
        assertFalse(response.isHasMoreRecords());
    }

    @Test
    void testCreateCaseLinkInfoListEmptyLinkReasonCaseLinkFieldsPresent() throws JsonProcessingException {
        final String caseDetails =
            String.format(CASE_DETAILS_TEMPLATE_EMPTY_LINK_REASON_CASE_LINK_FIELDS, "1500638105106660",
                "jurisdiction", "state", "caseTypeId", "caseNameHmctsInternal");

        CaseLinkRetrievalResults caseLinkRetrievalResults = CaseLinkRetrievalResults.builder()
            .caseDetails(List.of(OBJECT_MAPPER.readValue(caseDetails, CaseDetails.class)))
            .hasMoreResults(false)
            .build();

        final GetLinkedCasesResponse response = getLinkedCasesResponseCreator.createResponse(caseLinkRetrievalResults,
            CASE_REFERENCE);

        final List<CaseLinkInfo> caseLinks = response.getLinkedCases();

        assertFalse(response.getLinkedCases().isEmpty());
        assertEquals(caseLinks.size(), 1);
        assertEquals(caseLinks.get(0).getLinkDetails().size(), 3);
        assertFalse(response.isHasMoreRecords());
    }


    @Test
    void testCreateCaseLinkInfoListNoCaseNameHmctsInternalFieldPresent() throws JsonProcessingException {

        final String minimalCaseDetails = "{\n"
            + "  \"id\": \"1500638105106660\",\n"
            + "  \"jurisdiction\": \"jurisdiction\",\n"
            + "  \"state\": \"state\",\n"
            + "  \"version\": 1,\n"
            + "  \"case_type_id\": \"caseTypeId\",\n"
            + "  \"created_date\": \"2016-06-22T20:44:52.824\",\n"
            + "  \"last_modified\": \"2016-06-24T20:44:52.824\",\n"
            + "  \"last_state_modified_date\": \"2016-06-24T20:44:52.824\",\n"
            + "  \"security_classification\": \"PUBLIC\",\n"
            + "  \"case_data\": {\n"
            +        createCaseLinkCollectionString(STANDARD_CASE_LINK_FIELD, List.of(CASE_REFERENCE))
            + "  }\n"
            + "}";

        CaseLinkRetrievalResults caseLinkRetrievalResults = CaseLinkRetrievalResults.builder()
            .caseDetails(List.of(OBJECT_MAPPER.readValue(minimalCaseDetails, CaseDetails.class)))
            .hasMoreResults(false)
            .build();

        final GetLinkedCasesResponse response = getLinkedCasesResponseCreator.createResponse(caseLinkRetrievalResults,
            CASE_REFERENCE);

        assertFalse(response.getLinkedCases().isEmpty());
        assertNull(response.getLinkedCases().get(0).getCaseNameHmctsInternal());
    }

    void assertCaseLinkInfo(CaseLinkInfo caseLinkInfo, List<String> values) {
        assertEquals(values.get(0), caseLinkInfo.getCaseReference());
        assertEquals(values.get(1), caseLinkInfo.getCcdJurisdiction());
        assertEquals(values.get(2), caseLinkInfo.getState());
        assertEquals(values.get(3), caseLinkInfo.getCcdCaseType());
        assertEquals(values.get(4), caseLinkInfo.getCaseNameHmctsInternal());

        assertLinkDetailsList(caseLinkInfo.getLinkDetails());
    }

    void assertLinkDetailsList(List<CaseLinkDetails> caseLinkDetailsList) {

        // verify many case links can be processed for same case reference

        // NB: based on extract from CASE_DETAILS_TEMPLATE
        assertNotNull(caseLinkDetailsList);
        assertEquals(3, caseLinkDetailsList.size());

        // many reasons
        assertNotNull(caseLinkDetailsList.get(0).getCreatedDateTime());
        assertNotNull(caseLinkDetailsList.get(0).getReasons());
        assertEquals(2, caseLinkDetailsList.get(0).getReasons().size());
        assertEquals("Reason 1.1", caseLinkDetailsList.get(0).getReasons().get(0).getReasonCode());
        assertEquals("OtherDescription 1.1", caseLinkDetailsList.get(0).getReasons().get(0).getOtherDescription());
        assertEquals("Reason 1.2", caseLinkDetailsList.get(0).getReasons().get(1).getReasonCode());
        assertEquals("OtherDescription 1.2", caseLinkDetailsList.get(0).getReasons().get(1).getOtherDescription());

        // single reason
        assertNotNull(caseLinkDetailsList.get(1).getCreatedDateTime());
        assertNotNull(caseLinkDetailsList.get(1).getReasons());
        assertEquals(1, caseLinkDetailsList.get(1).getReasons().size());
        assertEquals("Reason 2.1", caseLinkDetailsList.get(1).getReasons().get(0).getReasonCode());
        assertEquals("OtherDescription 2.1", caseLinkDetailsList.get(1).getReasons().get(0).getOtherDescription());

        // minimal case link (i.e. no reasons or date time)
        assertNull(caseLinkDetailsList.get(2).getCreatedDateTime());
        assertNotNull(caseLinkDetailsList.get(2).getReasons());
        assertEquals(0, caseLinkDetailsList.get(2).getReasons().size());
    }
}
