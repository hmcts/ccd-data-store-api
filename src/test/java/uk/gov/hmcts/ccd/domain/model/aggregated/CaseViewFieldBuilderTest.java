package uk.gov.hmcts.ccd.domain.model.aggregated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

public class CaseViewFieldBuilderTest {

    private static final CaseField CASE_FIELD_2 = new CaseField();
    private static final CaseEventField EVENT_FIELD = new CaseEventField();
    private static final CaseEventField EVENT_FIELD_2 = new CaseEventField();
    private static final CaseEventField EVENT_FIELD_DYNAMIC_LIST = new CaseEventField();
    private static final String FIRST_NAME = "Patrick";
    private static final String LAST_NAME = "Smith";
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final AccessControlList acl1 = anAcl().withRole("role1").withCreate(true).withRead(true).withUpdate(true).withDelete(false).build();
    private static final AccessControlList acl2 = anAcl().withRole("role2").withCreate(true).withRead(true).withUpdate(false).withDelete(true).build();
    private static final AccessControlList acl3 = anAcl().withRole("role3").withCreate(false).withRead(false).withUpdate(true).withDelete(false).build();
    private static final FieldType textFieldType = aFieldType().withId("Text").withType("Text").build();
    private static final FieldType dynamicFieldType = aFieldType().withId("dynamicList").withType("DynamicList").build();
    private static final CaseField CASE_FIELD = newCaseField()
        .withFieldType(textFieldType)
        .withId("PersonFirstName")
        .withAcl(acl1)
        .withAcl(acl2)
        .withAcl(acl3)
        .build();
    private static final CaseField CASE_FIELD_DYNAMIC_LIST = newCaseField()
        .withFieldType(dynamicFieldType)
        .withId("dynamicList")
        .withAcl(acl1)
        .withAcl(acl2)
        .withAcl(acl3)
        .build();

    static {
        CASE_FIELD.setCaseTypeId("TestAddressBookCase");
        CASE_FIELD.setHidden(Boolean.FALSE);
        CASE_FIELD.setHintText("Some hint");
        CASE_FIELD.setLabel("First name");
        CASE_FIELD.setSecurityLabel("LO1");

        CASE_FIELD_2.setId("PersonLastName");

        EVENT_FIELD.setCaseFieldId("PersonFirstName");
        EVENT_FIELD.setDisplayContext("READONLY");
        EVENT_FIELD.setDisplayContext("#TABLE(Title, FirstName, MiddleName)");
        EVENT_FIELD.setShowCondition("ShowCondition");
        EVENT_FIELD.setShowSummaryChangeOption(Boolean.TRUE);
        EVENT_FIELD.setShowSummaryContentOption(3);

        EVENT_FIELD_2.setCaseFieldId("PersonLastName");

        EVENT_FIELD_DYNAMIC_LIST.setCaseFieldId("dynamicList");
    }

    private CaseViewFieldBuilder fieldBuilder;

    @Nested
    @DisplayName("Builder tests")
    class BuilderTest {
        @BeforeEach
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
            assertThat(field.getDisplayContextParameter(), is(EVENT_FIELD.getDisplayContextParamter()));
            assertThat(field.getShowCondition(), is(EVENT_FIELD.getShowCondition()));
            assertThat(field.getShowSummaryChangeOption(), is(Boolean.TRUE));
            assertThat(field.getShowSummaryContentOption(), is(3));
        }

        @Test
        public void shouldCreateFieldFromCaseEventFieldWithData() {
            final CaseViewField expectedField = getCaseViewField("FixedList");
            doReturn(expectedField).when(fieldBuilder).build(CASE_FIELD, EVENT_FIELD);

            final JsonNode data = JSON_NODE_FACTORY.textNode("value");

            final CaseViewField field = fieldBuilder.build(CASE_FIELD, EVENT_FIELD, data);

            verify(fieldBuilder).build(CASE_FIELD, EVENT_FIELD);
            assertThat(field, is(expectedField));
            assertThat(field.getValue(), equalTo(data));
        }

        @Test
        public void shouldCreateFieldFromCaseEventFieldWithDynamicListTypeData() throws Exception {
            final String expectedValue = "{\"value\": {\"code\":\"FixedList1\",\"label\":\"Fixed List 1\"}," +
                "\"list_items\": [{\"code\":\"FixedList1\",\"label\":\"Fixed List 1\"},{\"code\":\"FixedList2\",\"label\":\"Fixed List 2\"}," +
                "{\"code\":\"FixedList3\",\"label\":\"Fixed List 3\"},{\"code\":\"FixedList4\",\"label\":\"Fixed List 4\"}," +
                "{\"code\":\"FixedList5\",\"label\":\"Fixed List 5\"},{\"code\":\"FixedList6\",\"label\":\"Fixed List 6\"}," +
                "{\"code\":\"FixedList7\",\"label\":\"Fixed List 7\"}] }";


            String content = "{\n" + "          \"value\": {\n"
                + "            \"code\": \"FixedList1\",\n"
                + "            \"label\": \"Fixed List 1\"\n"
                + "          },\n"
                + "          \"list_items\": [{\n"
                + "            \"code\": \"FixedList1\",\n"
                + "            \"label\": \"Fixed List 1\"\n"
                + "          }, {\n"
                + "            \"code\": \"FixedList2\",\n"
                + "            \"label\": \"Fixed List 2\"\n"
                + "          }, {\n"
                + "            \"code\": \"FixedList3\",\n"
                + "            \"label\": \"Fixed List 3\"\n"
                + "          }, {\n"
                + "            \"code\": \"FixedList4\",\n"
                + "            \"label\": \"Fixed List 4\"\n"
                + "          }, {\n"
                + "            \"code\": \"FixedList5\",\n"
                + "            \"label\": \"Fixed List 5\"\n"
                + "          }, {\n"
                + "            \"code\": \"FixedList6\",\n"
                + "            \"label\": \"Fixed List 6\"\n"
                + "          }, {\n"
                + "            \"code\": \"FixedList7\",\n"
                + "            \"label\": \"Fixed List 7\"\n"
                + "          }\n"
                + "          ]\n"
                + "        }";
            final JsonNode data = new ObjectMapper().readTree(content);

            final CaseViewField field = fieldBuilder.build(CASE_FIELD_DYNAMIC_LIST, EVENT_FIELD, data);

            verify(fieldBuilder).build(CASE_FIELD_DYNAMIC_LIST, EVENT_FIELD);
            assertThat(field.getFieldType().getFixedListItems().size(), is(7));
            assertThat(field.getFieldType().getFixedListItems().get(0).getCode(), equalTo(expectedValue));
        }

        @Test
        public void shouldCreateFieldFromArrayOfCaseEventField() {
            final CaseViewField expectedField = getCaseViewField("FixedList");
            final CaseViewField expectedField2 = getCaseViewField("FixedList");

            doReturn(expectedField).when(fieldBuilder).build(CASE_FIELD, EVENT_FIELD);
            doReturn(expectedField2).when(fieldBuilder).build(CASE_FIELD_2, EVENT_FIELD_2);

            final List<CaseField> caseFields = asList(CASE_FIELD, CASE_FIELD_2);
            final List<CaseEventField> eventFields = asList(EVENT_FIELD, EVENT_FIELD_2);
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
            final CaseViewField expectedField = getCaseViewField("FixedList");

            doReturn(expectedField).when(fieldBuilder).build(CASE_FIELD, EVENT_FIELD);

            final List<CaseField> caseFields = asList(CASE_FIELD);
            final List<CaseEventField> eventFields = asList(EVENT_FIELD, EVENT_FIELD_2);
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
            final CaseViewField expectedField = getCaseViewField("FixedList");

            doReturn(expectedField).when(fieldBuilder).build(CASE_FIELD, EVENT_FIELD);

            final List<CaseField> caseFields = asList(CASE_FIELD, CASE_FIELD_2);
            final List<CaseEventField> eventFields = asList(EVENT_FIELD);
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
            final CaseViewField expectedField = getCaseViewField("FixedList");

            doReturn(expectedField).when(fieldBuilder).build(CASE_FIELD, EVENT_FIELD);

            final List<CaseField> caseFields = asList(CASE_FIELD);
            final List<CaseEventField> eventFields = asList(EVENT_FIELD);
            final Map<String, JsonNode> data = new HashMap<>();
            data.put("PersonLastName", JSON_NODE_FACTORY.textNode(LAST_NAME));

            final List<CaseViewField> fields = fieldBuilder.build(caseFields, eventFields, data);

            assertThat(fields, hasSize(1));
            assertThat(fields, contains(expectedField));

            verify(fieldBuilder).build(CASE_FIELD, EVENT_FIELD, null);
        }

        @Test
        public void shouldOverrideCaseFieldLabelAndHintWithEventFieldLabelAndHint() {
            String overriddenLabel = "overridden label";
            String overriddenHint = "overridden hint";
            CaseField caseField = new CaseField();
            caseField.setFieldType(textFieldType);
            CaseEventField caseEventField = new CaseEventField();
            caseEventField.setLabel(overriddenLabel);
            caseEventField.setHintText(overriddenHint);

            CaseViewField caseViewField = fieldBuilder.build(caseField, caseEventField);

            assertThat(caseViewField.getLabel(), is(overriddenLabel));
            assertThat(caseViewField.getHintText(), is(overriddenHint));
        }
    }

    private CaseViewField getCaseViewField(String type) {
        final CaseViewField expectedField = new CaseViewField();
        FieldType fieldType = new FieldType();
        fieldType.setType(type);
        expectedField.setFieldType(fieldType);
        return expectedField;
    }

    @Nested
    @DisplayName("ACL tests")
    class CaseViewFieldTest {
        private static final String TEXT_TYPE = "Text";
        private static final String YESNO_TYPE = "YesOrNo";

        private static final String FAMILY = "Family";
        private static final String MEMBERS = "Members";
        private static final String PERSON = "Person";
        private static final String NAME = "Name";
        private static final String SURNAME = "Surname";
        private static final String ADULT = "Adult";
        private static final String ADDRESS = "Address";
        private static final String ADDRESS_LINE = "Address Line";
        private static final String ADDRESS_LINES = "Address Lines";
        private static final String POSTCODE = "Post Code";
        private static final String FAMILY_NAME = "Family Name";

        private CaseField name = newCaseField().withId(NAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
        private CaseField surname = newCaseField().withId(SURNAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
        private CaseField adult = newCaseField().withId(ADULT).withFieldType(aFieldType().withId(YESNO_TYPE).withType(YESNO_TYPE).build()).build();
        private FieldType personFieldType = aFieldType().withId(PERSON).withType(COMPLEX).withComplexField(name).withComplexField(surname).withComplexField(adult).build();
        private CaseField person = newCaseField().withId(PERSON).withFieldType(personFieldType).build();
        private FieldType membersFieldType = aFieldType().withId(MEMBERS + "-some-uid-value").withType(COLLECTION).withCollectionField(person).build();
        private CaseField members = newCaseField().withId(MEMBERS).withFieldType(membersFieldType).build();

        private CaseField addressLine = newCaseField().withId(ADDRESS_LINE).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
        private FieldType addressLinesType = aFieldType().withId(ADDRESS_LINES).withType(COLLECTION).withCollectionField(addressLine).build();
        private CaseField addressLines = newCaseField().withId(ADDRESS_LINES).withFieldType(addressLinesType).build();
        private CaseField postCode = newCaseField().withId(POSTCODE).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
        private FieldType addressFieldType = aFieldType().withComplexField(addressLines).withComplexField(postCode).withId(ADDRESS).withType(COMPLEX).build();
        private CaseField address = newCaseField().withId(ADDRESS).withFieldType(addressFieldType).build();

        private CaseField familyName = newCaseField().withId(FAMILY_NAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
        private FieldType nameFieldType =
            aFieldType().withId(NAME + "-some-uid-value").withType(COLLECTION).withCollectionField(familyName).build();
        private CaseField familyNames = newCaseField().withId(FAMILY_NAME).withFieldType(nameFieldType).build();

        // A complex family field formed of members collection of complex person - text name, text surname and yesNo adult fields,
        // family name(text) and an address (complex address type - collection of text address lines and a text postCode)
        private FieldType familyFieldType =
            aFieldType().withId(FAMILY).withType(COMPLEX).withComplexField(familyNames).withComplexField(members).withComplexField(address).build();
        private AccessControlList acl1 = anAcl().withRole("role1").withCreate(true).withRead(true).withUpdate(true).withDelete(false).build();
        private AccessControlList acl2 = anAcl().withRole("role2").withCreate(true).withRead(true).withUpdate(false).withDelete(true).build();
        private AccessControlList acl3 = anAcl().withRole("role3").withCreate(false).withRead(false).withUpdate(true).withDelete(false).build();
        private CaseField family = newCaseField().withId(FAMILY).withFieldType(familyFieldType).withAcl(acl1).withAcl(acl2).withAcl(acl3).build();

        @BeforeEach
        public void setUp() {
            fieldBuilder = spy(new CaseViewFieldBuilder());
        }

        @Test
        @DisplayName("should pass ACLs to the children")
        void createFrom() {
            CaseViewField caseViewField = fieldBuilder.build(family, EVENT_FIELD);

            assertAll(
                () -> assertNotNull(caseViewField),
                () -> assertThat(caseViewField.getAccessControlLists().size(), is(3)),
                () -> assertThat(caseViewField.getFieldType().getComplexFields().get(0).getAccessControlLists().size(), is(3)),
                () -> assertThat(caseViewField.getFieldType().getComplexFields().get(0).getFieldType().getCollectionFieldType().getComplexFields().get(0)
                    .getAccessControlLists().size(), is(3)),
                () -> assertThat(caseViewField.getFieldType().getComplexFields().get(1).getFieldType().getCollectionFieldType().getComplexFields().get(0)
                    .getAccessControlLists().size(), is(3)),
                () -> assertThat(caseViewField.getFieldType().getComplexFields().get(2).getAccessControlLists().size(), is(3)),
                () -> assertThat(caseViewField.getFieldType().getComplexFields().get(1).getFieldType().getCollectionFieldType().getComplexFields().get(0)
                    .getAccessControlLists().size(), is(3)),
                () -> assertThat(caseViewField.getFieldType().getComplexFields().get(2).getFieldType().getComplexFields().get(0).getAccessControlLists().size(), is(3)),
                () -> assertThat(caseViewField.getFieldType().getComplexFields().get(2).getFieldType().getComplexFields().get(1).getAccessControlLists().size(), is(3))
            );

        }

        @Test
        @DisplayName("should propagateACLsToNestedFields to fix ACLs of the children")
        void callsPropagateACLsToNestedFields() {
            CaseField caseFieldMock = mock(CaseField.class);

            fieldBuilder.build(caseFieldMock, EVENT_FIELD);

            verify(caseFieldMock).propagateACLsToNestedFields();
        }
    }
}
