package uk.gov.hmcts.ccd.domain.service.caselinking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.ccd.WireMockBaseTest.CASE_01_ID;
import static uk.gov.hmcts.ccd.WireMockBaseTest.CASE_01_REFERENCE;
import static uk.gov.hmcts.ccd.WireMockBaseTest.CASE_02_ID;
import static uk.gov.hmcts.ccd.WireMockBaseTest.CASE_02_REFERENCE;
import static uk.gov.hmcts.ccd.WireMockBaseTest.CASE_03_ID;
import static uk.gov.hmcts.ccd.WireMockBaseTest.CASE_03_REFERENCE;
import static uk.gov.hmcts.ccd.WireMockBaseTest.CASE_04_ID;
import static uk.gov.hmcts.ccd.WireMockBaseTest.CASE_04_REFERENCE;

public abstract class CaseLinkTestFixtures {

    protected static final Long CASE_DATA_ID = 4321L;
    protected static final Long CASE_REFERENCE = 4444333322221111L;
    protected static final Long CASE_REFERENCE_02 = 1111222233334444L;
    protected static final String CASE_TYPE_ID = "Test";
    protected static final String CASE_TYPE_ID_02 = "Test-02";

    protected static final Long LINKED_CASE_REFERENCE_01 = Long.parseLong(CASE_01_REFERENCE);
    protected static final Long LINKED_CASE_REFERENCE_02 = Long.parseLong(CASE_02_REFERENCE);
    protected static final Long LINKED_CASE_REFERENCE_03 = Long.parseLong(CASE_03_REFERENCE);
    protected static final Long LINKED_CASE_REFERENCE_04 = Long.parseLong(CASE_04_REFERENCE);

    protected static final Long LINKED_CASE_DATA_ID_01 = CASE_01_ID;
    protected static final Long LINKED_CASE_DATA_ID_02 = CASE_02_ID;
    protected static final Long LINKED_CASE_DATA_ID_03 = CASE_03_ID;
    protected static final Long LINKED_CASE_DATA_ID_04 = CASE_04_ID;

    protected static final String LINKED_CASE_REFERENCE_VIA_SIMPLE_FIELD = CASE_01_REFERENCE;
    protected static final String LINKED_CASE_REFERENCE_VIA_COLLECTION = CASE_02_REFERENCE;
    protected static final String LINKED_CASE_REFERENCE_VIA_STANDARD_CASE_LINK_FIELD = CASE_03_REFERENCE;
    protected static final String LINKED_CASE_REFERENCE_VIA_BOTH_COL_AND_STANDARD_CL_FIELD = CASE_04_REFERENCE;

    protected void assertCaseLink(List<CaseLink> results,
                                  Long expectedLinkedCaseReference,
                                  Boolean expectedStandardCaseLinkFlag) {
        assertCaseLink(results, expectedLinkedCaseReference.toString(), expectedStandardCaseLinkFlag);
    }

    protected void assertCaseLink(List<CaseLink> results,
                                  String expectedLinkedCaseReference,
                                  boolean expectedStandardCaseLinkFlag) {
        CaseLink actualCaseLink = results.stream()
            .filter(caseLink -> expectedLinkedCaseReference.equals(caseLink.getLinkedCaseReference().toString()))
            .findAny().orElse(null);

        assertCaseLink(actualCaseLink, expectedLinkedCaseReference, expectedStandardCaseLinkFlag);
    }

    protected void assertCaseLink(CaseLink actualCaseLink,
                                  String expectedLinkedCaseReference,
                                  boolean expectedStandardCaseLinkFlag) {
        assertNotNull(actualCaseLink);

        assertEquals(CASE_REFERENCE, actualCaseLink.getCaseReference());

        assertEquals(expectedLinkedCaseReference, actualCaseLink.getLinkedCaseReference().toString());
        assertEquals(expectedStandardCaseLinkFlag, actualCaseLink.getStandardLink());
    }

    protected CaseDetails createCaseDetails(Map<String, JsonNode> caseData) {
        return createCaseDetails(CASE_REFERENCE, CASE_TYPE_ID, caseData);
    }

    protected CaseDetails createCaseDetails(Long caseReference, String caseType, Map<String, JsonNode> caseData) {
        CaseDetails caseDetails = new CaseDetails();

        caseDetails.setReference(caseReference);
        caseDetails.setCaseTypeId(caseType);
        caseDetails.setData(caseData);

        return caseDetails;
    }

    protected Map<String, JsonNode> createCaseDataMap(List<String> dataValues) throws JsonProcessingException {
        return JacksonUtils.MAPPER.readValue("{" + StringUtils.join(dataValues, ",") + "}",
            new TypeReference<HashMap<String, JsonNode>>() { });
    }

    protected static String createCaseLinkCollectionString(String fieldName, List<String> linkedCaseReferences) {
        return "\"" + fieldName + "\" : [ " + linkedCaseReferences.stream()
            .map(caseReferences -> "{\n"
               + "        \"id\" : \"" + UUID.randomUUID() + "\",\n"
               + "        \"value\" : {\n"
               + "          \"CaseReference\" : \"" + caseReferences + "\"\n"
               + "        }\n"
               + "      }")
            .collect(Collectors.joining(", ")) + " ]";
    }

    protected CaseTypeDefinition createCaseTypeDefinition() {
        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(CASE_TYPE_ID);
        caseTypeDefinition.setCaseFieldDefinitions(List.of(
            new CaseFieldDefinition(),
            new CaseFieldDefinition()
        ));
        return caseTypeDefinition;
    }

}
