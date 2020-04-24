package uk.gov.hmcts.ccd.domain.types.sanitiser;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

public class ComplexSanitiserTest {

    private static final JsonNodeFactory JSON_FACTORY = new JsonNodeFactory(false);

    private static final String TYPE_1 = "Type1";
    private static final FieldTypeDefinition FIELD_1_TYPE = new FieldTypeDefinition();
    private static final String FIELD_1_ID = "FirstName";
    private static final CaseFieldDefinition FIELD_1 = new CaseFieldDefinition();
    private static final JsonNode FIELD_1_VALUE_INITIAL = JSON_FACTORY.textNode("Initial value 1");
    private static final JsonNode FIELD_1_VALUE_SANITISED = JSON_FACTORY.textNode("Sanitised value 1");

    private static final String TYPE_2 = "Type2";
    private static final FieldTypeDefinition FIELD_2_TYPE = new FieldTypeDefinition();
    private static final String FIELD_2_ID = "Money";
    private static final CaseFieldDefinition FIELD_2 = new CaseFieldDefinition();
    private static final JsonNode FIELD_2_VALUE_INITIAL = JSON_FACTORY.textNode("Initial value 2");
    private static final JsonNode FIELD_2_VALUE_SANITISED = JSON_FACTORY.textNode("Sanitised value 2");

    private static final FieldTypeDefinition COMPLEX_FIELD_TYPE = new FieldTypeDefinition();

    static {
        FIELD_1_TYPE.setId(TYPE_1);
        FIELD_1_TYPE.setType(TYPE_1);
        FIELD_1.setId(FIELD_1_ID);
        FIELD_1.setFieldTypeDefinition(FIELD_1_TYPE);

        FIELD_2_TYPE.setId(TYPE_2);
        FIELD_2_TYPE.setType(TYPE_2);
        FIELD_2.setId(FIELD_2_ID);
        FIELD_2.setFieldTypeDefinition(FIELD_2_TYPE);

        COMPLEX_FIELD_TYPE.setComplexFields(Arrays.asList(FIELD_1, FIELD_2));
    }

    private Sanitiser type1Sanitiser;
    private Sanitiser type2Sanitiser;

    private ComplexSanitiser complexSanitiser;

    @Before
    public void setUp() {
        type1Sanitiser = mock(Sanitiser.class);
        doReturn(TYPE_1).when(type1Sanitiser).getType();

        type2Sanitiser = mock(Sanitiser.class);
        doReturn(TYPE_2).when(type2Sanitiser).getType();

        complexSanitiser = new ComplexSanitiser();
        complexSanitiser.setSanitisers(Arrays.asList(type1Sanitiser, type2Sanitiser));
    }

    @Test
    public void getType() {
        assertThat(complexSanitiser.getType(), equalTo(COMPLEX));
    }

    @Test
    public void shouldRecursivelySanitiseEveryChildren() {
        doReturn(FIELD_1_VALUE_SANITISED).when(type1Sanitiser).sanitise(FIELD_1_TYPE, FIELD_1_VALUE_INITIAL);
        doReturn(FIELD_2_VALUE_SANITISED).when(type2Sanitiser).sanitise(FIELD_2_TYPE, FIELD_2_VALUE_INITIAL);

        final ObjectNode complexData = JSON_FACTORY.objectNode();
        complexData.set(FIELD_1_ID, FIELD_1_VALUE_INITIAL);
        complexData.set(FIELD_2_ID, FIELD_2_VALUE_INITIAL);

        final JsonNode sanitisedData = complexSanitiser.sanitise(COMPLEX_FIELD_TYPE, complexData);

        verify(type1Sanitiser, times(1)).sanitise(FIELD_1_TYPE, FIELD_1_VALUE_INITIAL);
        verify(type2Sanitiser, times(1)).sanitise(FIELD_2_TYPE, FIELD_2_VALUE_INITIAL);
        assertThat(sanitisedData.get(FIELD_1_ID), is(FIELD_1_VALUE_SANITISED));
        assertThat(sanitisedData.get(FIELD_2_ID), is(FIELD_2_VALUE_SANITISED));
    }

    @Test
    public void shouldIgnoreUnknownFields() {
        final ObjectNode complexData = JSON_FACTORY.objectNode();
        complexData.set("UnknownField", FIELD_1_VALUE_INITIAL);

        final JsonNode sanitisedData = complexSanitiser.sanitise(COMPLEX_FIELD_TYPE, complexData);

        verify(type1Sanitiser, never()).sanitise(FIELD_1_TYPE, FIELD_1_VALUE_INITIAL);
        verify(type2Sanitiser, never()).sanitise(FIELD_2_TYPE, FIELD_2_VALUE_INITIAL);
        assertThat(sanitisedData.size(), equalTo(0));
    }

    @Test
    public void shouldLeaveFieldsWithoutSanitiserUntouched() {
        complexSanitiser.setSanitisers(Collections.emptyList());

        final ObjectNode complexData = JSON_FACTORY.objectNode();
        complexData.set(FIELD_1_ID, FIELD_1_VALUE_INITIAL);

        final JsonNode sanitisedData = complexSanitiser.sanitise(COMPLEX_FIELD_TYPE, complexData);

        verify(type1Sanitiser, never()).sanitise(FIELD_1_TYPE, FIELD_1_VALUE_INITIAL);
        assertThat(sanitisedData.get(FIELD_1_ID), is(FIELD_1_VALUE_INITIAL));
    }

}
