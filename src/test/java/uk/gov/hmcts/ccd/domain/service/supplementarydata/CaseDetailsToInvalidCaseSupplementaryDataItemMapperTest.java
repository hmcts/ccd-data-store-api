package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;

class CaseDetailsToInvalidCaseSupplementaryDataItemMapperTest {

    private static final Long CASE_REFERENCE1 = 123L;
    private static final Long CASE_REFERENCE2 = 456L;
    private static final String CASE_TYPE = "CaseType";
    private static final String JURISDICTION = "Jurisdiction";

    @Mock
    private Map<String, JsonNode> supplementaryData1;

    @Mock
    private Map<String, JsonNode> supplementaryData2;

    private final CaseDetailsToInvalidCaseSupplementaryDataItemMapper instance =
        new CaseDetailsToInvalidCaseSupplementaryDataItemMapper();

    @Test
    void shouldMapTwoCaseDetails() throws JsonProcessingException {
        List<CaseDetails> caseDetailsList = List.of(createCaseDetails1(), createCaseDetails2());

        List<InvalidCaseSupplementaryDataItem> result = instance.mapToDataItem(caseDetailsList);

        assertEquals(2, result.size());

        Optional<InvalidCaseSupplementaryDataItem> firstOptional = result.stream()
            .filter(e -> CASE_REFERENCE1.equals(e.getCaseId())).findFirst();
        assertTrue(firstOptional.isPresent());

        Optional<InvalidCaseSupplementaryDataItem> secondOptional = result.stream()
            .filter(e -> CASE_REFERENCE2.equals(e.getCaseId())).findFirst();
        assertTrue(secondOptional.isPresent());

        InvalidCaseSupplementaryDataItem first = firstOptional.get();
        InvalidCaseSupplementaryDataItem second = secondOptional.get();

        assertAll(
            () -> assertEquals(CASE_TYPE, first.getCaseTypeId()),
            () -> assertEquals(CASE_TYPE, second.getCaseTypeId()),
            () -> assertEquals(JURISDICTION, first.getJurisdiction()),
            () -> assertEquals(JURISDICTION, second.getJurisdiction()),
            () -> assertEquals(supplementaryData1, first.getSupplementaryData()),
            () -> assertEquals(supplementaryData2, second.getSupplementaryData()),

            () -> assertEquals("QUK123N", first.getApplicant1OrganisationPolicy()),
            () -> assertNull(first.getApplicant2OrganisationPolicy()),
            () -> assertEquals("QUK345N", first.getRespondent1OrganisationPolicy()),
            () -> assertEquals("QUK456N", first.getRespondent2OrganisationPolicy()),
            () -> assertEquals("UNSPEC_CLAIM", first.getCaseAccessCategory()),

            () -> assertNull(second.getApplicant1OrganisationPolicy()),
            () -> assertEquals("QUK987N", second.getApplicant2OrganisationPolicy()),
            () -> assertNull(second.getRespondent1OrganisationPolicy()),
            () -> assertNull(second.getRespondent2OrganisationPolicy()),
            () -> assertNull(second.getCaseAccessCategory())

        );
    }

    private CaseDetails createCaseDetails1() throws JsonProcessingException {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(CASE_REFERENCE1);
        caseDetails.setCaseTypeId(CASE_TYPE);
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setSupplementaryData(supplementaryData1);

        final JsonNode data = MAPPER.readTree("{"
            + "\"PersonLastName\":\"Roof\","
            + "\"PersonFirstName\":\"George\","
            + "\"applicant1OrganisationPolicy\": {\n"
            + "  \"OrgPolicyReference\": \"ClaimantPolicy\",\n"
            + "    \"Organisation\": {\n"
            + "      \"OrganisationID\": \"QUK123N\",\n"
            + "      \"OrganisationName\": \"CCD Solicitors Limited1\"\n"
            + "    }\n"
            + "  },\n"
            + "\"respondent1OrganisationPolicy\": {\n"
            + "  \"OrgPolicyReference\": \"DefendantPolicy\",\n"
            + "  \"Organisation\": {\n"
            + "    \"OrganisationID\": \"QUK345N\",\n"
            + "    \"OrganisationName\": \"CCD Solicitors Limited3\"\n"
            + "  }\n"
            + "},\n"
            + "\"respondent2OrganisationPolicy\": {\n"
            + "  \"OrgPolicyReference\": \"DefendantPolicy\",\n"
            + "  \"Organisation\": {\n"
            + "    \"OrganisationID\": \"QUK456N\",\n"
            + "    \"OrganisationName\": \"CCD Solicitors Limited4\"\n"
            + "  }\n"
            + "},"
            + "\"CaseAccessCategory\":\"UNSPEC_CLAIM\","
            + "\"PersonAddress\":{"
            + "\"Country\":\"Wales\","
            + "\"Postcode\":\"WB11DDF\","
            + "\"AddressLine1\":\"Flat 9\","
            + "\"AddressLine2\":\"2 Hubble Avenue\","
            + "\"AddressLine3\":\"ButtonVillie\"}"
            + "}");

        caseDetails.setData(JacksonUtils.convertValue(data));
        return caseDetails;
    }

    private CaseDetails createCaseDetails2() throws JsonProcessingException {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(CASE_REFERENCE2);
        caseDetails.setCaseTypeId(CASE_TYPE);
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setSupplementaryData(supplementaryData2);

        final JsonNode data = MAPPER.readTree("{"
            + "\"PersonLastName\":\"Smith\","
            + "\"PersonFirstName\":\"Joe\","
            + "\"applicant2OrganisationPolicy\": {\n"
            + "  \"OrgPolicyReference\": \"DefendantPolicy\",\n"
            + "  \"Organisation\": {\n"
            + "    \"OrganisationID\": \"QUK987N\",\n"
            + "    \"OrganisationName\": \"CCD Solicitors Limited2\"\n"
            + "  }\n"
            + "},\n"
            + "\"PersonAddress\":{"
            + "\"Country\":\"Wales\","
            + "\"Postcode\":\"WB11DDF\","
            + "\"AddressLine1\":\"Flat 9\","
            + "\"AddressLine2\":\"2 Hubble Avenue\","
            + "\"AddressLine3\":\"ButtonVillie\"}"
            + "}");

        caseDetails.setData(JacksonUtils.convertValue(data));
        return caseDetails;
    }


}
