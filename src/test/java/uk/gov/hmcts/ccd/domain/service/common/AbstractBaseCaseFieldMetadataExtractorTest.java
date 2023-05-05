package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.doReturn;

abstract class AbstractBaseCaseFieldMetadataExtractorTest extends TestFixtures {

    protected static final String DATA_FIELD_ID = "DocumentField";
    protected static final String DATA_FIELD_VALUE = "{\n"
        + "    \"document_url\": \"http://dm-store:8080/documents/a2c2f1f9-c309-4060-8f77-1800be0ca885\",\n"
        + "    \"document_filename\": \"A_Simple Document.docx\",\n"
        + "\"document_binary_url\": \"http://dm-store:8080/documents/a2c2f1f9-c309-4060-8f77-1800be0ca885/binary\",\n"
        + "    \"category_id\": null\n"
        + "  }";
    protected static final String FIELD_TYPE_ID = "DocumentField";
    protected static final String BASE_FIELD_TYPE_ID = "Document";
    protected static final String FIELD_TYPE_ID_GENERATED = "DocumentField-45af7864-091f-4b48-979d-1a7e5900ecd4";

    protected static JsonNode dataValue;
    protected static Map.Entry<String, JsonNode> nodeEntry;

    @BeforeAll
    static void prepare() throws Exception {
        final CaseDefinitionRepository caseDefinitionRepository = Mockito.mock(CaseDefinitionRepository.class);
        BaseType.setCaseDefinitionRepository(caseDefinitionRepository);
        final List<FieldTypeDefinition> fieldTypeDefinitions = TestFixtures.getFieldTypesFromJson("base-types.json");

        doReturn(fieldTypeDefinitions).when(caseDefinitionRepository).getBaseTypes();

        fieldTypeDefinitions.forEach(fieldType -> BaseType.register(new BaseType(fieldType)));

        dataValue = mapper.readTree(DATA_FIELD_VALUE);
        nodeEntry = new AbstractMap.SimpleEntry<>(DATA_FIELD_ID, dataValue);
    }

    protected static Stream<Arguments> provideNullParameters() {
        final CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();

        return Stream.of(
            Arguments.of(null, caseFieldDefinition, "timeline.0.", FIELD_TYPE_ID, emptyList()),
            Arguments.of(nodeEntry, null, "timeline.0.", FIELD_TYPE_ID, emptyList()),
            Arguments.of(nodeEntry, caseFieldDefinition, null, FIELD_TYPE_ID, emptyList()),
            Arguments.of(nodeEntry, caseFieldDefinition, "timeline.0.", null, emptyList()),
            Arguments.of(nodeEntry, caseFieldDefinition, "timeline.0.", FIELD_TYPE_ID, null)
        );
    }

}
