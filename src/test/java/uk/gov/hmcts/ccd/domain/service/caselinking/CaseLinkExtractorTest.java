package uk.gov.hmcts.ccd.domain.service.caselinking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataExtractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkExtractor.TEXT_CASE_REFERENCE;

@ExtendWith(MockitoExtension.class)
class CaseLinkExtractorTest extends CaseLinkTestFixtures {

    private static final String CASE_FIELDS_JSON = "/tests/CaseLinkExtractor_CaseFields.json";
    private static List<CaseFieldDefinition> caseFieldDefinitions;

    @Mock
    private CaseDataExtractor caseDataExtractor;

    @InjectMocks
    private CaseLinkExtractor caseLinkExtractor;

    private static final String CASE_LINK_VIA_SIMPLE_FIELD =
        "\"CaseLinkField\" : {\n"
            + "        \"CaseReference\" : \"" + LINKED_CASE_REFERENCE_VIA_SIMPLE_FIELD + "\"\n"
            + "      }";

    private static final String CASE_LINK_VIA_COLLECTION =
        "\"CaseLinkCollection\" : [ {\n"
            + "        \"id\" : \"" + UUID.randomUUID() + "\",\n"
            + "        \"value\" : {\n"
            + "          \"CaseReference\" : \"" + LINKED_CASE_REFERENCE_VIA_COLLECTION_01 + "\"\n"
            + "        }\n"
            + "      }, {\n"
            + "        \"id\" : \"" + UUID.randomUUID() + "\",\n"
            + "        \"value\" : {\n"
            + "          \"CaseReference\" : \"" + LINKED_CASE_REFERENCE_VIA_COLLECTION_02 + "\"\n"
            + "        }\n"
            + "      } ]";

    @BeforeEach
    void setup() throws IOException {
        caseFieldDefinitions = WireMockBaseTest.getCaseFieldsFromJson(BaseTest.getResourceAsString(CASE_FIELDS_JSON));
    }

    @DisplayName("Should extract a simple CaseLink value")
    @Test
    void shouldExtractSimpleCaseLinkValue() throws JsonProcessingException {

        // ARRANGE
        CaseDetails caseDetails = createCaseDetailsAndMockCaseDetailsExtractor(true, false);

        // ACT
        List<String> results = caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // ASSERT
        assertEquals(1, results.size());
        assertTrue(results.contains(LINKED_CASE_REFERENCE_VIA_SIMPLE_FIELD));

    }

    @DisplayName("Should extract CaseLink values from a collection")
    @Test
    void shouldExtractCollectionCaseLinkValues() throws JsonProcessingException {

        // ARRANGE
        CaseDetails caseDetails = createCaseDetailsAndMockCaseDetailsExtractor(false, true);

        // ACT
        List<String> results = caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // ASSERT
        assertEquals(2, results.size());
        assertTrue(results.contains(LINKED_CASE_REFERENCE_VIA_COLLECTION_01));
        assertTrue(results.contains(LINKED_CASE_REFERENCE_VIA_COLLECTION_02));

    }

    @DisplayName(
        "Should extract CaseLink values even if some of the values are empty or null"
    )
    @Test
    void shouldExtractEvenIfSomeCaseLinkFieldsAreNullOrEmpty() throws JsonProcessingException {

        // ARRANGE
        List<String> dataValues = List.of(
            "\"CaseLinkField\" : null",
            "\"CaseLinkCollection\" : [\n"
                + "      {\n"
                + "        \"id\" : \"" + UUID.randomUUID() + "\",\n"
                + "        \"value\" : {\n"
                + "          \"CaseReference\" : \"\"\n" // empty
                + "        }\n"
                + "      },"
                + "      {\n"
                + "        \"id\" : \"" + UUID.randomUUID() + "\",\n"
                + "        \"value\" : {\n"
                + "          \"CaseReference\" : null\n"  // null
                + "        }\n"
                + "      },"
                + "      null,"  // missing
                + "      {\n"
                + "        \"id\" : \"" + UUID.randomUUID() + "\",\n"
                + "        \"value\" : {\n"
                + "          \"CaseReference\" : \"4444333322221111\"\n"  // good
                + "        }\n"
                + "      }\n"
                + "     ]"
        );
        CaseDetails caseDetails = createCaseDetails(createCaseDataMap(dataValues));
        // mock path extract based on the above
        when(caseDataExtractor.extractFieldTypePaths(anyMap(), eq(caseFieldDefinitions), eq(TEXT_CASE_REFERENCE)))
            .thenReturn(List.of(
                "CaseLinkField",
                "CaseLinkCollection.0.value.CaseReference",
                "CaseLinkCollection.1.value.CaseReference",
                "CaseLinkCollection.2.value.CaseReference",
                "CaseLinkCollection.3.value.CaseReference"
            ));

        // ACT
        List<String> results = caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // ASSERT
        assertEquals(1, results.size());
        assertTrue(results.contains("4444333322221111"));
    }

    private Map<String, JsonNode> createCaseData(boolean includeSimpleCaseLink,
                                                 boolean includeCollectionOfCaseLinks) throws JsonProcessingException {

        List<String> dataValues = new ArrayList<>();
        if (includeSimpleCaseLink) {
            dataValues.add(CASE_LINK_VIA_SIMPLE_FIELD);
        }
        if (includeCollectionOfCaseLinks) {
            dataValues.add(CASE_LINK_VIA_COLLECTION);
        }

        return createCaseDataMap(dataValues);
    }

    private Map<String, JsonNode> createCaseDataMap(List<String> dataValues) throws JsonProcessingException {
        return JacksonUtils.MAPPER.readValue("{" + StringUtils.join(dataValues, ",") + "}",
            new TypeReference<HashMap<String, JsonNode>>() { });
    }

    private CaseDetails createCaseDetailsAndMockCaseDetailsExtractor(boolean includeSimpleCaseLink,
                                                                     boolean includeCollectionOfCaseLinks)
        throws JsonProcessingException {

        mockCaseDetailsExtractor(includeSimpleCaseLink, includeCollectionOfCaseLinks);

        return createCaseDetails(
            createCaseData(includeSimpleCaseLink, includeCollectionOfCaseLinks)
        );
    }

    private void mockCaseDetailsExtractor(boolean includeSimpleCaseLink,
                                          boolean includeCollectionOfCaseLinks) {
        List<String> dataPaths = new ArrayList<>();

        if (includeSimpleCaseLink) {
            dataPaths.add("CaseLinkField");
        }
        if (includeCollectionOfCaseLinks) {
            dataPaths.add("CaseLinkCollection.0.value.CaseReference");
            dataPaths.add("CaseLinkCollection.1.value.CaseReference");
        }

        when(caseDataExtractor.extractFieldTypePaths(anyMap(), eq(caseFieldDefinitions), eq(TEXT_CASE_REFERENCE)))
            .thenReturn(dataPaths);
    }

}
