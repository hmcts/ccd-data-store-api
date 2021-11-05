package uk.gov.hmcts.ccd.domain.service.casedeletion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseDataExtractorTest extends WireMockBaseTest {

    private static final String CASE_FIELD_JSON = "/tests/CaseDataValidator_CaseField.json";
    private static List<CaseFieldDefinition> caseFields;

    private CaseDataExtractor caseDataExtractor = new CaseDataExtractor();

    private static final String SIMPLE_DATA =
        "{\n" +
            "  \"CaseReference\" : \"Address Line 1\"\n" +
            "}";

    private static final String COLLECTION_DATA =
        "{\n" +
            "  \"CaseLink\" : [\n" +
            "    {\n" +
            "      \"value\": " +
            "        {\n" +
            "          \"CaseLink1\": \"1596104840593131\",\n" +
            "          \"CaseLink2\": \"1596104840593131\"\n" +
            "        }\n" +
            "    }" +
            "  ]\n" +
            "}";

    private static final String COMPLEX_OBJECT_DATA =
        "{\n" +
            "   \"Person\": {\n" +
            "        \"Name\": \"NameValue\",\n" +
            "        \"Address\": {\n" +
            "            \"Line1\": \"Address Line1\"\n," +
            "            \"Line2\": \"Address Line1\"\n" +
            "         }\n" +
            "  }\n" +
            "}";

    @ParameterizedTest
    @MethodSource("provideExtractFieldTypePathsParameters")
    void extractFieldTypePathsFromSimpleObject(String data, List<String> expectedFieldTypePaths) throws Exception {
        final Map<String, JsonNode> jsonData
            = JacksonUtils.MAPPER.readValue(data, new TypeReference<HashMap<String, JsonNode>>() { });
        caseFields = getCaseFieldsFromJson(BaseTest.getResourceAsString(CASE_FIELD_JSON));

        List<String> extractedFieldTypePaths = caseDataExtractor.extractFieldTypePaths(jsonData, caseFields, "Text");

        assertEquals(expectedFieldTypePaths.size(), extractedFieldTypePaths.size());
        assertTrue(extractedFieldTypePaths.containsAll(expectedFieldTypePaths));
    }

    private static Stream<Arguments> provideExtractFieldTypePathsParameters() {
        return Stream.of(
            Arguments.of(SIMPLE_DATA, List.of("CaseReference")),
            Arguments.of(COLLECTION_DATA, List.of("CaseLink.0.CaseLink1", "CaseLink.0.CaseLink2")),
            Arguments.of(COMPLEX_OBJECT_DATA, List.of("Person.Name", "Person.Address.Line1", "Person.Address.Line2"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideExtractDocumentFieldTypePathsParameters")
    void extractDocumentFieldTypePaths(String definitionFileName, List<String> expectedPaths) throws IOException {
        final String readString =
            Files.readString(Paths.get("src/test/resources/tests/CaseDataExtractorDocumentData.json"));
        caseFields =
            getCaseFieldsFromJson(BaseTest.getResourceAsString(String.format("/tests/%s", definitionFileName)));

        final Map<String, JsonNode> data
            = JacksonUtils.MAPPER.readValue(readString, new TypeReference<HashMap<String, JsonNode>>() { });

        List<String> extractedFieldTypePaths = caseDataExtractor.extractFieldTypePaths(data, caseFields, "Document");

        assertNotNull(extractedFieldTypePaths);
        assertEquals(expectedPaths.size(), extractedFieldTypePaths.size());
        assertTrue(extractedFieldTypePaths.containsAll(expectedPaths));
    }

    private static Stream<Arguments> provideExtractDocumentFieldTypePathsParameters() {
        return Stream.of(
            Arguments.of("DocumentCaseTypeDefinitions_draftOrderDoc.json", List.of("draftOrderDoc")),
            Arguments.of("DocumentCaseTypeDefinitions_evidence.json", List.of("evidence.type")),
            Arguments.of("DocumentCaseTypeDefinitions_extractDocUploadList.json",
                List.of("extraDocUploadList.0", "extraDocUploadList.1")),
            Arguments.of("DocumentCaseTypeDefinitions_state.json",
                List.of("state.0.partyDetail.0.type",
                    "state.0.partyDetail.1.type",
                    "state.1.partyDetail.0.type",
                    "state.1.partyDetail.1.type",
                    "state.2.partyDetail.0.type",
                    "state.2.partyDetail.1.type",
                    "state.2.partyDetail.2.type")),
            Arguments.of("DocumentCaseTypeDefinitions_timeline.json", List.of("timeline.0.type", "timeline.1.type"))
        );
    }
}
