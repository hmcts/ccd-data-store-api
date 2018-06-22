package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class CaseViewFieldBuilderTest {

    private static final CaseField CASE_FIELD = new CaseField();
    private static final CaseField CASE_FIELD_2 = new CaseField();
    private static final CaseEventField EVENT_FIELD = new CaseEventField();
    private static final CaseEventField EVENT_FIELD_2 = new CaseEventField();
    private static final String FIRST_NAME = "Patrick";
    private static final String LAST_NAME = "Smith";
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    static {
        CASE_FIELD.setId("PersonFirstName");
        CASE_FIELD.setCaseTypeId("TestAddressBookCase");
        CASE_FIELD.setFieldType(new FieldType());
        CASE_FIELD.setHidden(Boolean.FALSE);
        CASE_FIELD.setHintText("Some hint");
        CASE_FIELD.setLabel("First name");
        CASE_FIELD.setSecurityLabel("LO1");

        CASE_FIELD_2.setId("PersonLastName");

        EVENT_FIELD.setCaseFieldId("PersonFirstName");
        EVENT_FIELD.setDisplayContext("READONLY");
        EVENT_FIELD.setShowCondition("ShowCondition");
        EVENT_FIELD.setShowSummaryChangeOption(Boolean.TRUE);
        EVENT_FIELD.setShowSummaryContentOption(3);

        EVENT_FIELD_2.setCaseFieldId("PersonLastName");
    }

    private CaseViewFieldBuilder fieldBuilder;

    @Before
    public void setUp() {
        fieldBuilder = spy(new CaseViewFieldBuilder());
    }

    @Test
    public void shouldCreateFieldFromCaseEventField() {

        final CaseViewField field = fieldBuilder.build(CASE_FIELD, EVENT_FIELD);

        assertThat(field, is(notNullValue()));
        assertThat(field.getId(), equalTo(CASE_FIELD.getId()));
        assertThat(field.getFieldType(), equalTo(CASE_FIELD.getFieldType()));
        assertThat(field.isHidden(), equalTo(CASE_FIELD.getHidden()));
        assertThat(field.getHintText(), equalTo(CASE_FIELD.getHintText()));
        assertThat(field.getLabel(), equalTo(CASE_FIELD.getLabel()));
        assertThat(field.getOrder(), is(nullValue()));
        assertThat(field.getSecurityLabel(), equalTo(CASE_FIELD.getSecurityLabel()));
        assertThat(field.getValidationExpression(), is(nullValue()));
        assertThat(field.getDisplayContext(), is(EVENT_FIELD.getDisplayContext()));
        assertThat(field.getShowCondition(), is(EVENT_FIELD.getShowCondition()));
        assertThat(field.getShowSummaryChangeOption(), is(Boolean.TRUE));
        assertThat(field.getShowSummaryContentOption(), is(3));
    }

    @Test
    public void shouldCreateFieldFromCaseEventFieldWithData() {
        final CaseViewField expectedField = new CaseViewField();
        doReturn(expectedField).when(fieldBuilder).build(CASE_FIELD, EVENT_FIELD);

        final JsonNode data = JSON_NODE_FACTORY.textNode("value");

        final CaseViewField field = fieldBuilder.build(CASE_FIELD, EVENT_FIELD, data);

        verify(fieldBuilder).build(CASE_FIELD, EVENT_FIELD);
        assertThat(field, is(expectedField));
        assertThat(field.getValue(), equalTo(data));
    }

    @Test
    public void shouldCreateFieldFromArrayOfCaseEventField() {
        final CaseViewField expectedField = new CaseViewField();
        final CaseViewField expectedField2 = new CaseViewField();

        doReturn(expectedField).when(fieldBuilder).build(CASE_FIELD, EVENT_FIELD);
        doReturn(expectedField2).when(fieldBuilder).build(CASE_FIELD_2, EVENT_FIELD_2);

        final List<CaseField> caseFields = Arrays.asList(CASE_FIELD, CASE_FIELD_2);
        final List<CaseEventField> eventFields = Arrays.asList(EVENT_FIELD, EVENT_FIELD_2);
        final Map<String, JsonNode> data = new HashMap<>();
        data.put("PersonFirstName", JSON_NODE_FACTORY.textNode(FIRST_NAME));
        data.put("PersonLastName", JSON_NODE_FACTORY.textNode(LAST_NAME));

        final List<CaseViewField> fields = fieldBuilder.build(caseFields, eventFields, data);

        assertThat(fields, hasSize(2));
        assertThat(fields, contains(expectedField, expectedField2));

        verify(fieldBuilder).build(CASE_FIELD, EVENT_FIELD, data.get("PersonFirstName"));
        verify(fieldBuilder).build(CASE_FIELD_2, EVENT_FIELD_2, data.get("PersonLastName"));
    }

    @Test
    public void shouldCreateFieldFromArrayOfCaseEventField_fieldNotInCaseFieldsIsIgnored() {
        final CaseViewField expectedField = new CaseViewField();

        doReturn(expectedField).when(fieldBuilder).build(CASE_FIELD, EVENT_FIELD);

        final List<CaseField> caseFields = Arrays.asList(CASE_FIELD);
        final List<CaseEventField> eventFields = Arrays.asList(EVENT_FIELD, EVENT_FIELD_2);
        final Map<String, JsonNode> data = new HashMap<>();
        data.put("PersonFirstName", JSON_NODE_FACTORY.textNode(FIRST_NAME));
        data.put("PersonLastName", JSON_NODE_FACTORY.textNode(LAST_NAME));

        final List<CaseViewField> fields = fieldBuilder.build(caseFields, eventFields, data);

        assertThat(fields, hasSize(1));
        assertThat(fields, contains(expectedField));

        verify(fieldBuilder).build(CASE_FIELD, EVENT_FIELD, data.get("PersonFirstName"));
        verify(fieldBuilder, times(1)).build(Mockito.any(CaseField.class), Mockito.any(CaseEventField.class), any());
    }

    @Test
    public void shouldCreateFieldFromArrayOfCaseEventField_fieldNotInEventFieldsIsIgnored() {
        final CaseViewField expectedField = new CaseViewField();

        doReturn(expectedField).when(fieldBuilder).build(CASE_FIELD, EVENT_FIELD);

        final List<CaseField> caseFields = Arrays.asList(CASE_FIELD, CASE_FIELD_2);
        final List<CaseEventField> eventFields = Arrays.asList(EVENT_FIELD);
        final Map<String, JsonNode> data = new HashMap<>();
        data.put("PersonFirstName", JSON_NODE_FACTORY.textNode(FIRST_NAME));
        data.put("PersonLastName", JSON_NODE_FACTORY.textNode(LAST_NAME));

        final List<CaseViewField> fields = fieldBuilder.build(caseFields, eventFields, data);

        assertThat(fields, hasSize(1));
        assertThat(fields, contains(expectedField));

        verify(fieldBuilder).build(CASE_FIELD, EVENT_FIELD, data.get("PersonFirstName"));
        verify(fieldBuilder, times(1)).build(Mockito.any(CaseField.class), Mockito.any(CaseEventField.class), any());
    }

    @Test
    public void shouldCreateFieldFromArrayOfCaseEventField_fieldWithoutData() {
        final CaseViewField expectedField = new CaseViewField();

        doReturn(expectedField).when(fieldBuilder).build(CASE_FIELD, EVENT_FIELD);

        final List<CaseField> caseFields = Arrays.asList(CASE_FIELD);
        final List<CaseEventField> eventFields = Arrays.asList(EVENT_FIELD);
        final Map<String, JsonNode> data = new HashMap<>();
        data.put("PersonLastName", JSON_NODE_FACTORY.textNode(LAST_NAME));

        final List<CaseViewField> fields = fieldBuilder.build(caseFields, eventFields, data);

        assertThat(fields, hasSize(1));
        assertThat(fields, contains(expectedField));

        verify(fieldBuilder).build(CASE_FIELD, EVENT_FIELD, null);
    }

}
