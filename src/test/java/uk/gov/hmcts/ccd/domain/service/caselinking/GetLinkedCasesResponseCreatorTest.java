package uk.gov.hmcts.ccd.domain.service.caselinking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinkInfo;
import uk.gov.hmcts.ccd.domain.model.caselinking.GetLinkedCasesResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
        + "  \"CaseLink\": {\n"
        + "    \"CaseReference\": \"" + CASE_REFERENCE + "\",\n"
        + "    \"CaseType\": \"MyBaseType\",\n"
        + "    \"CreatedDateTime\": \"2022-03-28T14:35:48.645045\",\n"
        + "    \"ReasonForLink\": ["
        + "      {\n"
        + "        \"value\": {\n"
        + "          \"Reason\": \"reason\",\n"
        + "          \"OtherDescription\": \"otherDescription\"\n"
        + "        },\n"
        + "        \"id\": \"4eddd3e0-1cf0-4ab3-9783-9d9a96c16db\"\n"
        + "      },"
        + "      {\n"
        + "        \"value\": {\n"
        + "          \"Reason\": \"reason2\",\n"
        + "          \"OtherDescription\": \"otherDescription2\"\n"
        + "        },\n"
        + "        \"id\": \"6b9cff58-af9d-11ec-b909-0242ac120002\"\n"
        + "      }"
        + "     ]\n"
        + "  },\n"
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
        + "  \"delete_draft_response_status\": null,\n"
        + "  \"security_classifications\": {\n"
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
        + "  }\n"
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
        + "  \"delete_draft_response_status\": null,\n"
        + "  \"security_classifications\": {\n"
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
        + "  }\n"
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
    void testCreateCaseLinkInfos() throws JsonProcessingException {
        List<String> caseDetails1 = List.of("1500638105106660",
            "jusrisdiction",
            "state",
            "caseTypeId",
            "caseNameHmctsInternal");
        List<String> caseDetails2 = List.of("9514840069336542",
            "jusrisdiction2",
            "state2",
            "caseTypeId2",
            "caseNameHmctsInternal2");
        List<String> caseDetails3 = List.of("4827897342988773",
            "jusrisdiction3",
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
    void testCreateCaseLinkInfosNoCaseLinkFieldsPresent() throws JsonProcessingException {
        final String caseDetails = String.format(CASE_DETAILS_TEMPLATE_NO_CASE_LINK_FIELDS, "1500638105106660",
            "jusrisdiction","state",  "caseTypeId", "caseNameHmctsInternal");

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
    void testCreateCaseLinkInfosNoCaseNameHmctsInternalFieldPresent() throws JsonProcessingException {

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
            + "     \"CaseLink\": {\n"
            + "         \"CaseReference\": \"" + CASE_REFERENCE + "\",\n"
            + "         \"CaseType\": \"MyBaseType\",\n"
            + "         \"CreatedDateTime\": \"2022-03-28T14:35:48.645045\",\n"
            + "         \"ReasonForLink\": ["
            + "             {\n"
            + "                 \"value\": {\n"
            + "                     \"Reason\": \"reason2\",\n"
            + "                     \"OtherDescription\": \"otherDescription2\"\n"
            + "                 },\n"
            + "                 \"id\": \"6b9cff58-af9d-11ec-b909-0242ac120002\"\n"
            + "             }"
            + "         ]\n"
            + "     }\n"
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
        assertEquals(caseLinkInfo.getCaseReference(), values.get(0));
        assertEquals(caseLinkInfo.getCcdJurisdiction(), values.get(1));
        assertEquals(caseLinkInfo.getState(), values.get(2));
        assertEquals(caseLinkInfo.getCcdCaseType(), values.get(3));
        assertEquals(caseLinkInfo.getCaseNameHmctsInternal(), values.get(4));
    }
}
