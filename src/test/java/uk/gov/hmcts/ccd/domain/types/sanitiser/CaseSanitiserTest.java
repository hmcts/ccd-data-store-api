package uk.gov.hmcts.ccd.domain.types.sanitiser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class CaseSanitiserTest {

    private static final JsonNodeFactory JSON_FACTORY = new JsonNodeFactory(false);

    private static final CaseTypeDefinition CASE_TYPE = new CaseTypeDefinition();

    private static final String TYPE_SIMPLE = "Simple";
    private static final FieldTypeDefinition SIMPLE_FIELD_TYPE = new FieldTypeDefinition();
    private static final String SIMPLE_FIELD_ID = "FirstName";
    private static final CaseFieldDefinition SIMPLE_FIELD = new CaseFieldDefinition();
    private static final JsonNode SIMPLE_VALUE_INITIAL = JSON_FACTORY.textNode("Initial value");
    private static final JsonNode SIMPLE_VALUE_SANITISED = JSON_FACTORY.textNode("Sanitised value");
    private static final String TYPE_OTHER = "Other";

    static {
        SIMPLE_FIELD_TYPE.setId(TYPE_SIMPLE);
        SIMPLE_FIELD_TYPE.setType(TYPE_SIMPLE);
        SIMPLE_FIELD.setId(SIMPLE_FIELD_ID);
        SIMPLE_FIELD.setFieldTypeDefinition(SIMPLE_FIELD_TYPE);

        CASE_TYPE.setCaseFieldDefinitions(Collections.singletonList(SIMPLE_FIELD));
    }

    private CaseSanitiser caseSanitiser;
    private Sanitiser simpleSanitiser;

    @Before
    public void setUp() throws Exception {
        simpleSanitiser = mock(Sanitiser.class);
        doReturn(TYPE_SIMPLE).when(simpleSanitiser).getType();

        caseSanitiser = new CaseSanitiser(Collections.singletonList(simpleSanitiser));
    }

    @Test
    public void shouldSanitiseDataForSimpleFieldsUsingSimpleSanitiser() {
        doReturn(SIMPLE_VALUE_SANITISED).when(simpleSanitiser).sanitise(SIMPLE_FIELD_TYPE, SIMPLE_VALUE_INITIAL);

        final HashMap<String, JsonNode> caseData = new HashMap<>();
        caseData.put(SIMPLE_FIELD_ID, SIMPLE_VALUE_INITIAL);

        final Map<String, JsonNode> sanitisedData = caseSanitiser.sanitise(CASE_TYPE, caseData);

        verify(simpleSanitiser).sanitise(SIMPLE_FIELD_TYPE, SIMPLE_VALUE_INITIAL);
        assertThat(sanitisedData.get(SIMPLE_FIELD_ID), is(SIMPLE_VALUE_SANITISED));
    }

    @Test
    public void shouldLeaveFieldsWithoutSanitiserUntouched() {
        doReturn(TYPE_OTHER).when(simpleSanitiser).getType();
        caseSanitiser = new CaseSanitiser(Collections.singletonList(simpleSanitiser));

        final HashMap<String, JsonNode> caseData = new HashMap<>();
        caseData.put(SIMPLE_FIELD_ID, SIMPLE_VALUE_INITIAL);

        final Map<String, JsonNode> sanitisedData = caseSanitiser.sanitise(CASE_TYPE, caseData);

        verify(simpleSanitiser, never()).sanitise(any(), any());
        assertThat(sanitisedData.get(SIMPLE_FIELD_ID), is(SIMPLE_VALUE_INITIAL));
    }

}
