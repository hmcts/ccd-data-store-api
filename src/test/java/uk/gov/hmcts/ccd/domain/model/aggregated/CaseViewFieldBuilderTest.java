package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.READONLY;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

public class CaseViewFieldBuilderTest {

    private static final CaseField CASE_FIELD_2 = new CaseField();
    private static final CaseEventField EVENT_FIELD = new CaseEventField();
    private static final CaseEventField EVENT_FIELD_2 = new CaseEventField();
    private static final CaseEventField EVENT_FIELD_3 = new CaseEventField();
    private static final String FIRST_NAME = "Patrick";
    private static final String LAST_NAME = "Smith";
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final AccessControlList acl1 = anAcl().withRole("role1").withCreate(true).withRead(true).withUpdate(true).withDelete(false).build();
    private static final AccessControlList acl2 = anAcl().withRole("role2").withCreate(true).withRead(true).withUpdate(false).withDelete(true).build();
    private static final AccessControlList acl3 = anAcl().withRole("role3").withCreate(false).withRead(false).withUpdate(true).withDelete(false).build();
    private static final FieldType textFieldType = aFieldType().withId("Text").withType("Text").build();
    private static final CaseField CASE_FIELD = newCaseField()
        .withFieldType(textFieldType)
        .withId("PersonFirstName")
        .withAcl(acl1)
        .withAcl(acl2)
        .withAcl(acl3)
        .build();
    private static final CaseField CASE_FIELD_3 = newCaseField().withFieldType(textFieldType).withId("STATE").build();
    private static final String TEXT_TYPE = "Text";

    static {
        CASE_FIELD.setCaseTypeId("TestAddressBookCase");
        CASE_FIELD.setHidden(Boolean.FALSE);
        CASE_FIELD.setHintText("Some hint");
        CASE_FIELD.setLabel("First name");
        CASE_FIELD.setSecurityLabel("LO1");
        CASE_FIELD.setMetadata(false);
        CASE_FIELD.setFormattedValue("DisplayValue");

        CASE_FIELD_2.setId("PersonLastName");

        CASE_FIELD_3.setMetadata(true);

        EVENT_FIELD.setCaseFieldId("PersonFirstName");
        EVENT_FIELD.setDisplayContext(READONLY);
        EVENT_FIELD.setDisplayContextParameter("#TABLE(Title, FirstName, MiddleName)");
        EVENT_FIELD.setShowCondition("ShowCondition");
        EVENT_FIELD.setShowSummaryChangeOption(Boolean.TRUE);
        EVENT_FIELD.setShowSummaryContentOption(3);

        EVENT_FIELD_2.setCaseFieldId("PersonLastName");
        EVENT_FIELD_3.setCaseFieldId("State");
    }

    private CompoundFieldOrderService compoundFieldOrderService = new CompoundFieldOrderService();
    private CaseViewFieldBuilder fieldBuilder;

    @Nested
    @DisplayName("Builder tests")
    class BuilderTest {
        @BeforeEach
        public void setUp() {
            fieldBuilder = spy(new CaseViewFieldBuilder(compoundFieldOrderService));
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
            assertThat(field.getDisplayContextParameter(), is(EVENT_FIELD.getDisplayContextParameter()));
            assertThat(field.getShowCondition(), is(EVENT_FIELD.getShowCondition()));
            assertThat(field.getShowSummaryChangeOption(), is(Boolean.TRUE));
            assertThat(field.getShowSummaryContentOption(), is(3));
            assertThat(field.isMetadata(), is(false));
            assertThat(field.getFormattedValue(), is(CASE_FIELD.getFormattedValue()));

            CaseViewField metadataField = fieldBuilder.build(CASE_FIELD_3, EVENT_FIELD_3);
            assertThat(metadataField.isMetadata(), is(true));
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
            final CaseViewField expectedField = new CaseViewField();

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
            final CaseViewField expectedField = new CaseViewField();

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
            final CaseViewField expectedField = new CaseViewField();

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

    @Nested
    @DisplayName("ACL tests")
    class CaseViewFieldACLTest {
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

        private final CaseField name = newCaseField().withId(NAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
        private final CaseField surname = newCaseField().withId(SURNAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
        private final CaseField adult = newCaseField().withId(ADULT).withFieldType(aFieldType().withId(YESNO_TYPE).withType(YESNO_TYPE).build()).build();
        private final FieldType personFieldType = aFieldType()
            .withId(PERSON)
            .withType(COMPLEX)
            .withComplexField(name)
            .withComplexField(surname)
            .withComplexField(adult)
            .build();
        private final CaseField person = newCaseField().withId(PERSON).withFieldType(personFieldType).build();
        private final FieldType membersFieldType = aFieldType().withId(MEMBERS + "-some-uid-value").withType(COLLECTION).withCollectionField(person).build();
        private final CaseField members = newCaseField().withId(MEMBERS).withFieldType(membersFieldType).build();

        private final CaseField addressLine = newCaseField()
            .withId(ADDRESS_LINE)
            .withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build())
            .build();
        private final FieldType addressLinesType = aFieldType().withId(ADDRESS_LINES).withType(COLLECTION).withCollectionField(addressLine).build();
        private final CaseField addressLines = newCaseField().withId(ADDRESS_LINES).withFieldType(addressLinesType).build();
        private final CaseField postCode = newCaseField().withId(POSTCODE).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
        private final FieldType addressFieldType = aFieldType()
            .withComplexField(addressLines)
            .withComplexField(postCode)
            .withId(ADDRESS)
            .withType(COMPLEX)
            .build();
        private final CaseField address = newCaseField().withId(ADDRESS).withFieldType(addressFieldType).build();

        private final CaseField familyName = newCaseField()
            .withId(FAMILY_NAME)
            .withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build())
            .build();
        private final FieldType nameFieldType =
            aFieldType().withId(NAME + "-some-uid-value").withType(COLLECTION).withCollectionField(familyName).build();
        private final CaseField familyNames = newCaseField().withId(FAMILY_NAME).withFieldType(nameFieldType).build();

        // A complex family field formed of members collection of complex person - text name, text surname and yesNo adult fields,
        // family name(text) and an address (complex address type - collection of text address lines and a text postCode)
        private final FieldType familyFieldType =
            aFieldType().withId(FAMILY).withType(COMPLEX).withComplexField(familyNames).withComplexField(members).withComplexField(address).build();
        private final AccessControlList acl1 = anAcl().withRole("role1").withCreate(true).withRead(true).withUpdate(true).withDelete(false).build();
        private final AccessControlList acl2 = anAcl().withRole("role2").withCreate(true).withRead(true).withUpdate(false).withDelete(true).build();
        private final AccessControlList acl3 = anAcl().withRole("role3").withCreate(false).withRead(false).withUpdate(true).withDelete(false).build();
        private final CaseField family = newCaseField().withId(FAMILY).withFieldType(familyFieldType).withAcl(acl1).withAcl(acl2).withAcl(acl3).build();

        @BeforeEach
        public void setUp() {
            fieldBuilder = spy(new CaseViewFieldBuilder(compoundFieldOrderService));
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
