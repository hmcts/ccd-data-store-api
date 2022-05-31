package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.TestFixtures.caseDataFromJsonString;
import static uk.gov.hmcts.ccd.TestFixtures.getCaseFieldsFromJson;
import static uk.gov.hmcts.ccd.TestFixtures.loadCaseDataFromJson;

class CaseDataExtractorTest {

    private static final String CASE_FIELD_JSON = "tests/CaseDataExtractor_CaseField.json";
    private static List<CaseFieldDefinition> caseFields;

    private static final String SIMPLE_DATA =
        "{\n"
            + "  \"CaseReference\" : \"Address Line 1\"\n"
            + "}";

    private static final String COLLECTION_DATA =
        "{\n"
            + "  \"CaseLink\" : [\n"
            + "    {\n"
            + "      \"value\": "
            + "        {\n"
            + "          \"CaseLink1\": \"1596104840593131\",\n"
            + "          \"CaseLink2\": \"1596104840593131\"\n"
            + "        }\n"
            + "    }"
            + "  ]\n"
            + "}";

    private static final String COMPLEX_OBJECT_DATA =
        "{\n"
            + "   \"Person\": {\n"
            + "        \"CaseLink1\": \"1596104840593131\",\n"
            + "        \"Address\": {\n"
            + "            \"Line1\": \"Address Line1\"\n,"
            + "            \"Line2\": \"Address Line1\"\n"
            + "         }\n"
            + "  }\n"
            + "}";

    private static final String COMPLEX_OBJECT_DATA_TEST =
        "{\n"
            + "   \"Person\": {\n"
            + "        \"Name\": \"NameValue\",\n"
            + "        \"Address\": {\n"
            + "            \"Line1\": \"Address Line1\"\n,"
            + "            \"Line2\": \"Address Line1\"\n"
            + "         }\n"
            + "  },\n"
            + "   \"CaseLink1\": {\n"
            + "        \"CaseReference\": \"1596104840593131\"\n"
            + "    },"
            + "   \"CaseLink2\": {\n"
            + "        \"CaseReference\": \"1596104840593131\"\n"
            + "    }"
            + "}";

    private final CaseFieldMetadataExtractor simpleTypePathFinder = new SimpleCaseTypeMetadataExtractor();
    private final CaseFieldMetadataExtractor complexTypePathFinder = new ComplexCaseTypeMetadataExtractor();

    private final CaseDataExtractor underTest = new CaseDataExtractor(simpleTypePathFinder, complexTypePathFinder);

    @BeforeAll
    static void prepare() throws Exception {
        final CaseDefinitionRepository caseDefinitionRepository = Mockito.mock(CaseDefinitionRepository.class);
        BaseType.setCaseDefinitionRepository(caseDefinitionRepository);
        final List<FieldTypeDefinition> fieldTypeDefinitions = TestFixtures.getFieldTypesFromJson("base-types.json");

        doReturn(fieldTypeDefinitions).when(caseDefinitionRepository).getBaseTypes();

        fieldTypeDefinitions.forEach(fieldType -> BaseType.register(new BaseType(fieldType)));
    }

    @ParameterizedTest
    @MethodSource("provideExtractFieldTypePathsParameters")
    void extractFieldTypePathsFromSimpleObject(final String data,
                                               final List<CaseFieldMetadata> expectedResults) throws Exception {
        final Map<String, JsonNode> jsonData = caseDataFromJsonString(data);

        caseFields = getCaseFieldsFromJson(CASE_FIELD_JSON);

        final List<CaseFieldMetadata> results =
            underTest.extractFieldTypePaths(jsonData, caseFields, "TextCaseReference");

        assertThat(results)
            .isNotEmpty()
            .hasSize(expectedResults.size())
            .hasSameElementsAs(expectedResults);
    }

    private static Stream<Arguments> provideExtractFieldTypePathsParameters() {
        return Stream.of(
            Arguments.of(SIMPLE_DATA, List.of(new CaseFieldMetadata("CaseReference", null))),
            Arguments.of(COLLECTION_DATA, List.of(new CaseFieldMetadata("CaseLink.0.CaseLink1", null),
                new CaseFieldMetadata("CaseLink.0.CaseLink2", null))),
            Arguments.of(COMPLEX_OBJECT_DATA, List.of(new CaseFieldMetadata("Person.CaseLink1", null))),
            Arguments.of(COMPLEX_OBJECT_DATA_TEST, List.of(new CaseFieldMetadata("CaseLink1.CaseReference", null),
                new CaseFieldMetadata("CaseLink2.CaseReference", null)))
        );
    }

    @ParameterizedTest
    @MethodSource("provideExtractDocumentFieldTypePathsParameters")
    void extractDocumentFieldTypePaths(final String definitionFileName,
                                       final List<CaseFieldMetadata> expectedPaths) throws Exception {
        caseFields = getCaseFieldsFromJson(String.format("tests/%s", definitionFileName));

        final Map<String, JsonNode> data =
            loadCaseDataFromJson(String.format("tests/%s", "CaseDataExtractorDocumentData.json"));

        final List<CaseFieldMetadata> results = underTest.extractFieldTypePaths(data, caseFields, "Document");

        assertThat(results)
            .isNotEmpty()
            .hasSize(expectedPaths.size())
            .hasSameElementsAs(expectedPaths);
    }

    private static Stream<Arguments> provideExtractDocumentFieldTypePathsParameters() {
        return Stream.of(
            Arguments.of("DocumentCaseTypeDefinitions_draftOrderDoc.json",
                List.of(new CaseFieldMetadata("draftOrderDoc", null))),
            Arguments.of("DocumentCaseTypeDefinitions_evidence.json",
                List.of(new CaseFieldMetadata("evidence.type", null))),
            Arguments.of("DocumentCaseTypeDefinitions_extractDocUploadList.json",
                List.of(new CaseFieldMetadata("extraDocUploadList.0", null),
                    new CaseFieldMetadata("extraDocUploadList.1", null))),
            Arguments.of("DocumentCaseTypeDefinitions_state.json",
                List.of(new CaseFieldMetadata("state.0.partyDetail.0.type", null),
                    new CaseFieldMetadata("state.0.partyDetail.1.type", null),
                    new CaseFieldMetadata("state.1.partyDetail.0.type", null),
                    new CaseFieldMetadata("state.1.partyDetail.1.type", null),
                    new CaseFieldMetadata("state.2.partyDetail.0.type", null),
                    new CaseFieldMetadata("state.2.partyDetail.1.type", null),
                    new CaseFieldMetadata("state.2.partyDetail.2.type", null))),
            Arguments.of("DocumentCaseTypeDefinitions_timeline.json",
                List.of(new CaseFieldMetadata("timeline.0.type", null),
                    new CaseFieldMetadata("timeline.1.type", null)))
        );
    }

    @Test
    public void extractFieldTypeUnknownType() throws Exception {
        final Map<String, JsonNode> jsonData = caseDataFromJsonString(SIMPLE_DATA);

        caseFields = getCaseFieldsFromJson(CASE_FIELD_JSON);

        final List<CaseFieldMetadata> results = underTest.extractFieldTypePaths(jsonData, caseFields, "Unknown");

        assertThat(results).isEmpty();
    }

    @Test
    void extractFieldTypeTypeNotFound() throws Exception {
        final Map<String, JsonNode> jsonData = caseDataFromJsonString(SIMPLE_DATA);

        caseFields = getCaseFieldsFromJson(CASE_FIELD_JSON);

        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setType("CustomType");

        caseFieldDefinition.setId("id");
        caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);

        caseFields.add(caseFieldDefinition);

        final List<CaseFieldMetadata> results = underTest.extractFieldTypePaths(jsonData, caseFields, "CustomType");

        assertThat(results).isEmpty();
    }
}
