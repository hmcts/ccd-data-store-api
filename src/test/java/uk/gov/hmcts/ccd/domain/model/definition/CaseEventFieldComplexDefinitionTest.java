package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static uk.gov.hmcts.ccd.TestFixtures.fromFileAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseEventFieldComplexDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsAndWritesDisplayContextParameterWithinComplexEventFieldData() throws Exception {
        String json = fromFileAsString("tests/case-event-field-complex-display-context-parameter.json");
        CaseEventFieldDefinition eventField = objectMapper.readValue(json, CaseEventFieldDefinition.class);
        CaseEventFieldComplexDefinition definition = eventField.getCaseEventFieldComplexDefinitions().get(0);

        assertEquals("#ARGUMENT(CATEGORY-LEGAL-OPS,CATEGORY-ADMIN,REGION-1235)",
            definition.getDisplayContextParameter());
        assertEquals("#ARGUMENT(CATEGORY-LEGAL-OPS,CATEGORY-ADMIN,REGION-1235)",
            objectMapper.readTree(objectMapper.writeValueAsString(eventField))
            .get("case_fields_complex").get(0)
            .get("display_context_parameter").asText());
        assertEquals(definition.getDisplayContextParameter(), definition.createCopy().getDisplayContextParameter());
    }
}
