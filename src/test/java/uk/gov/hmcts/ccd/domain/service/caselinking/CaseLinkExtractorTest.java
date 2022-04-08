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
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity.NON_STANDARD_LINK;
import static uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity.STANDARD_LINK;
import static uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkExtractor.STANDARD_CASE_LINK_FIELD;
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
            + "          \"CaseReference\" : \"" + LINKED_CASE_REFERENCE_VIA_COLLECTION + "\"\n"
            + "        }\n"
            + "      }, {\n"
            + "        \"id\" : \"" + UUID.randomUUID() + "\",\n"
            + "        \"value\" : {\n"
            + "          \"CaseReference\" : \"" + LINKED_CASE_REFERENCE_VIA_BOTH_COL_AND_STANDARD_CL_FIELD + "\"\n"
            + "        }\n"
            + "      } ]";

    private static final String CASE_LINK_VIA_STANDARD_CASE_LINKS_FIELD =
        "\"" + STANDARD_CASE_LINK_FIELD + "\" : [ {\n"
            + "        \"id\" : \"" + UUID.randomUUID() + "\",\n"
            + "        \"value\" : {\n"
            + "          \"CaseReference\" : \"" + LINKED_CASE_REFERENCE_VIA_STANDARD_CASE_LINK_FIELD + "\"\n"
            + "        }\n"
            + "      }, {\n"
            + "        \"id\" : \"" + UUID.randomUUID() + "\",\n"
            + "        \"value\" : {\n"
            + "          \"CaseReference\" : \"" + LINKED_CASE_REFERENCE_VIA_BOTH_COL_AND_STANDARD_CL_FIELD + "\"\n"
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
        CaseDetails caseDetails = createCaseDetailsAndMockCaseDetailsExtractor(true, false, false);

        // ACT
        List<CaseLink> results = caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // ASSERT
        assertEquals(1, results.size());
        assertCaseLink(results.get(0), LINKED_CASE_REFERENCE_VIA_SIMPLE_FIELD, NON_STANDARD_LINK);

    }

    @DisplayName("Should extract CaseLink values from a collection")
    @Test
    void shouldExtractCollectionCaseLinkValues() throws JsonProcessingException {

        // ARRANGE
        CaseDetails caseDetails = createCaseDetailsAndMockCaseDetailsExtractor(false, true, false);

        // ACT
        List<CaseLink> results = caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // ASSERT
        assertEquals(2, results.size());
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_COLLECTION, NON_STANDARD_LINK);
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_BOTH_COL_AND_STANDARD_CL_FIELD, NON_STANDARD_LINK);

    }

    @DisplayName("Should extract CaseLink values from the standard `caseLinks` field: and set the StandardLink flag")
    @Test
    void shouldExtractStandardCaseLinkValuesAndSetTheFlag() throws JsonProcessingException {

        // ARRANGE
        CaseDetails caseDetails = createCaseDetailsAndMockCaseDetailsExtractor(false, false, true);

        // ACT
        List<CaseLink> results = caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // ASSERT
        assertEquals(2, results.size());
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_STANDARD_CASE_LINK_FIELD, STANDARD_LINK);
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_BOTH_COL_AND_STANDARD_CL_FIELD, STANDARD_LINK);

    }

    @DisplayName(
        "Should extract CaseLink values duplicated both inside and outside of the standard `caseLinks` field: "
            + "and set the StandardLink flag correctly"
    )
    @Test
    void shouldExtractWithCorrectStandardCaseLinkFlagsWhenLinksDuplicatedInsideAndOutsideOfSCLField()
        throws JsonProcessingException {

        // ARRANGE
        CaseDetails caseDetails = createCaseDetailsAndMockCaseDetailsExtractor(false, true, true);

        // ACT
        List<CaseLink> results = caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // ASSERT
        assertEquals(3, results.size());
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_COLLECTION, NON_STANDARD_LINK);
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_STANDARD_CASE_LINK_FIELD, STANDARD_LINK);
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_BOTH_COL_AND_STANDARD_CL_FIELD, STANDARD_LINK);
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
                + "     ]",
            "\"" + STANDARD_CASE_LINK_FIELD + "\" : null"
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
        List<CaseLink> results = caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // ASSERT
        assertEquals(1, results.size());
        assertCaseLink(results, "4444333322221111", NON_STANDARD_LINK);
    }

    @DisplayName("Should extract when Standard CaseLink field not in use")
    @Test
    void shouldExtractWhenStandardCaseLinkFieldNotConfigured()
        throws JsonProcessingException {

        // ARRANGE
        removeCaseFieldDefinition(STANDARD_CASE_LINK_FIELD);

        CaseDetails caseDetails = createCaseDetailsAndMockCaseDetailsExtractor(true, true, true);

        // ACT
        List<CaseLink> results = caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // ASSERT
        assertEquals(4, results.size());
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_SIMPLE_FIELD, NON_STANDARD_LINK);
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_COLLECTION, NON_STANDARD_LINK);
        // NB: still exported (as in test paths output) but flags not set
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_STANDARD_CASE_LINK_FIELD, NON_STANDARD_LINK);
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_BOTH_COL_AND_STANDARD_CL_FIELD, NON_STANDARD_LINK);
    }

    @DisplayName(
        "Should extract without setting StandardLink flag if Standard CaseLink field mis-configured: Not a collection"
    )
    @Test
    void shouldExtractWithoutSettingStandardLinkFlagWhenSCLFieldMisConfigured_NotACollection()
        throws JsonProcessingException {

        // ARRANGE
        // remove and replace the SCL field
        removeCaseFieldDefinition(STANDARD_CASE_LINK_FIELD);
        CaseFieldDefinition caseFieldDefinition = removeCaseFieldDefinition("TextField");
        caseFieldDefinition.setId(STANDARD_CASE_LINK_FIELD);
        caseFieldDefinitions.add(caseFieldDefinition);

        CaseDetails caseDetails = createCaseDetailsAndMockCaseDetailsExtractor(true, true, true);

        // ACT
        List<CaseLink> results = caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // ASSERT
        assertEquals(4, results.size());
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_SIMPLE_FIELD, NON_STANDARD_LINK);
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_COLLECTION, NON_STANDARD_LINK);
        // NB: still exported (as in test paths output) but flags not set
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_STANDARD_CASE_LINK_FIELD, NON_STANDARD_LINK);
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_BOTH_COL_AND_STANDARD_CL_FIELD, NON_STANDARD_LINK);
    }

    @DisplayName(
        "Should extract without setting StandardLink flag if Standard CaseLink field mis-configured: Wrong collection"
    )
    @Test
    void shouldExtractWithoutSettingStandardLinkFlagWhenSCLFieldMisConfigured_WrongCollection()
        throws JsonProcessingException {

        // ARRANGE
        // find and adjust the SCL field (wrong collection type)
        CaseFieldDefinition caseFieldDefinition = findCaseFieldDefinition(STANDARD_CASE_LINK_FIELD);
        caseFieldDefinition.getFieldTypeDefinition().getCollectionFieldTypeDefinition().setId("DifferentCollectionID");

        CaseDetails caseDetails = createCaseDetailsAndMockCaseDetailsExtractor(true, true, true);

        // ACT
        List<CaseLink> results = caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // ASSERT
        assertEquals(4, results.size());
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_SIMPLE_FIELD, NON_STANDARD_LINK);
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_COLLECTION, NON_STANDARD_LINK);
        // NB: still exported (as in test paths output) but flags not set
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_STANDARD_CASE_LINK_FIELD, NON_STANDARD_LINK);
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_BOTH_COL_AND_STANDARD_CL_FIELD, NON_STANDARD_LINK);
    }

    @DisplayName(
        "Should extract without setting StandardLink flag if Standard CaseLink field mis-configured: Bad collection"
    )
    @Test
    void shouldExtractWithoutSettingStandardLinkFlagWhenSCLFieldMisConfigured_BadCollection()
        throws JsonProcessingException {

        // ARRANGE
        // find and adjust the SCL field (delete collection field type config)
        CaseFieldDefinition caseFieldDefinition = removeCaseFieldDefinition(STANDARD_CASE_LINK_FIELD);
        caseFieldDefinition.getFieldTypeDefinition().setCollectionFieldTypeDefinition(null);
        caseFieldDefinitions.add(caseFieldDefinition);

        CaseDetails caseDetails = createCaseDetailsAndMockCaseDetailsExtractor(true, true, true);

        // ACT
        caseLinkExtractor = new CaseLinkExtractor(caseDataExtractor);
        List<CaseLink> results = caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);

        // ASSERT
        assertEquals(4, results.size());
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_SIMPLE_FIELD, NON_STANDARD_LINK);
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_COLLECTION, NON_STANDARD_LINK);
        // NB: still exported (as in test paths output) but flags not set
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_STANDARD_CASE_LINK_FIELD, NON_STANDARD_LINK);
        assertCaseLink(results, LINKED_CASE_REFERENCE_VIA_BOTH_COL_AND_STANDARD_CL_FIELD, NON_STANDARD_LINK);
    }

    private Map<String, JsonNode> createCaseData(boolean includeSimpleCaseLink,
                                                 boolean includeCollectionOfCaseLinks,
                                                 boolean includeStandardCaseLinksField) throws JsonProcessingException {

        List<String> dataValues = new ArrayList<>();
        if (includeSimpleCaseLink) {
            dataValues.add(CASE_LINK_VIA_SIMPLE_FIELD);
        }
        if (includeCollectionOfCaseLinks) {
            dataValues.add(CASE_LINK_VIA_COLLECTION);
        }
        if (includeStandardCaseLinksField) {
            dataValues.add(CASE_LINK_VIA_STANDARD_CASE_LINKS_FIELD);
        }

        return createCaseDataMap(dataValues);
    }

    private Map<String, JsonNode> createCaseDataMap(List<String> dataValues) throws JsonProcessingException {
        return JacksonUtils.MAPPER.readValue("{" + StringUtils.join(dataValues, ",") + "}",
            new TypeReference<HashMap<String, JsonNode>>() { });
    }

    private CaseDetails createCaseDetailsAndMockCaseDetailsExtractor(boolean includeSimpleCaseLink,
                                                                     boolean includeCollectionOfCaseLinks,
                                                                     boolean includeStandardCaseLinksField)
        throws JsonProcessingException {

        mockCaseDetailsExtractor(includeSimpleCaseLink, includeCollectionOfCaseLinks, includeStandardCaseLinksField);

        return createCaseDetails(
            createCaseData(includeSimpleCaseLink, includeCollectionOfCaseLinks, includeStandardCaseLinksField)
        );
    }

    private void mockCaseDetailsExtractor(boolean includeSimpleCaseLink,
                                          boolean includeCollectionOfCaseLinks,
                                          boolean includeStandardCaseLinksField) {
        List<String> dataPaths = new ArrayList<>();

        if (includeSimpleCaseLink) {
            dataPaths.add("CaseLinkField");
        }
        if (includeCollectionOfCaseLinks) {
            dataPaths.add("CaseLinkCollection.0.value.CaseReference");
            dataPaths.add("CaseLinkCollection.1.value.CaseReference");
        }
        if (includeStandardCaseLinksField) {
            dataPaths.add(STANDARD_CASE_LINK_FIELD + ".0.value.CaseReference");
            dataPaths.add(STANDARD_CASE_LINK_FIELD + ".1.value.CaseReference");
        }

        when(caseDataExtractor.extractFieldTypePaths(anyMap(), eq(caseFieldDefinitions), eq(TEXT_CASE_REFERENCE)))
            .thenReturn(dataPaths);
    }

    private CaseFieldDefinition findCaseFieldDefinition(String id) {
        return caseFieldDefinitions.stream()
            .filter(caseFieldDefinition -> id.equals(caseFieldDefinition.getId()))
            .findAny().orElse(null);
    }

    private CaseFieldDefinition removeCaseFieldDefinition(String id) {
        CaseFieldDefinition caseFieldDefinition = findCaseFieldDefinition(id);

        caseFieldDefinitions.remove(caseFieldDefinition);

        return caseFieldDefinition;
    }

}
