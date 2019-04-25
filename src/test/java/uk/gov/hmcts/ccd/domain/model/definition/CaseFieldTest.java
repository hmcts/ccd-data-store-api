package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class CaseFieldTest {

    private static final String TEXT_TYPE = "Text";
    private static final String FAMILY = "Family";
    private static final String MEMBERS = "Members";
    private static final String PERSON = "Person";
    private static final String DEBTOR_DETAILS = "Debtor details";
    private static final String NAME = "Name";
    private static final String SURNAME = "Surname";
    private static final String FAMILY_NAME = "Family Name";

    private CaseField name = newCaseField().withId(NAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private CaseField surname = newCaseField().withId(SURNAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private FieldType personFieldType = aFieldType().withId(PERSON).withType(COMPLEX).withComplexField(name).withComplexField(surname).build();
    private CaseField person = newCaseField().withId(PERSON).withFieldType(personFieldType).build();

    private FieldType debtorFieldType = aFieldType().withId(DEBTOR_DETAILS).withType(COMPLEX).withComplexField(person).build();
    private CaseField debtorDetails = newCaseField().withId(DEBTOR_DETAILS).withFieldType(debtorFieldType).build();

    private FieldType membersFieldType = aFieldType().withId(MEMBERS + "-some-uid-value").withType(COLLECTION).withCollectionField(person).build();
    private CaseField members = newCaseField().withId(MEMBERS).withFieldType(membersFieldType).build();

    private CaseField familyName = newCaseField().withId(FAMILY_NAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private FieldType nameFieldType =
        aFieldType().withId(NAME + "-some-uid-value").withType(COLLECTION).withCollectionField(familyName).build();
    private CaseField familyNames = newCaseField().withId(FAMILY_NAME).withFieldType(nameFieldType).build();

    private FieldType familyFieldType =
        aFieldType().withId(FAMILY).withType(COMPLEX).withComplexField(familyNames).withComplexField(members).build();
    private AccessControlList acl1 = anAcl().withRole("role1").withCreate(true).withRead(true).withUpdate(true).withDelete(false).build();
    private AccessControlList acl2 = anAcl().withRole("role2").withCreate(true).withRead(true).withUpdate(false).withDelete(true).build();
    private AccessControlList acl3 = anAcl().withRole("role3").withCreate(false).withRead(false).withUpdate(true).withDelete(false).build();
    private CaseField family = newCaseField().withId(FAMILY).withFieldType(familyFieldType).withAcl(acl1).withAcl(acl2).withAcl(acl3).build();

    @Nested
    @DisplayName("propagateACLsToNestedFields test")
    class CaseFieldSetChildrenACLsTest {

        @Test
        void propagateACLsToNestedFields() {

            family.propagateACLsToNestedFields();

            CaseField familyNames = family.getFieldType().getChildren().stream()
                .filter(e -> e.getId().equals(FAMILY_NAME)).findFirst().get();
            CaseField familyName = familyNames.getFieldType().getChildren().stream()
                .filter(e -> e.getId().equals(FAMILY_NAME)).findFirst().get();

            CaseField members = family.getFieldType().getChildren().stream()
                .filter(e -> e.getId().equals(MEMBERS)).findFirst().get();
            CaseField person = members.getFieldType().getChildren().stream()
                .filter(e -> e.getId().equals(PERSON)).findFirst().get();

            CaseField name = person.getFieldType().getChildren().stream()
                .filter(e -> e.getId().equals(NAME)).findFirst().get();
            CaseField surname = person.getFieldType().getChildren().stream()
                .filter(e -> e.getId().equals(SURNAME)).findFirst().get();

            assertAll(
                () -> assertThat(family.getAccessControlLists().size(), is(3)),
                () -> assertThat(familyName.getAccessControlLists().size(), is(3)),
                () -> assertThat(familyNames.getAccessControlLists().size(), is(3)),
                () -> assertThat(members.getAccessControlLists().size(), is(3)),
                () -> assertThat(person.getAccessControlLists().size(), is(3)),
                () -> assertThat(name.getAccessControlLists().size(), is(3)),
                () -> assertThat(surname.getAccessControlLists().size(), is(3)));
        }
    }

    @Nested
    @DisplayName("find by path tests")
    class FindNestedElementsTest {

        @Test
        void findNestedElementByPath() {
            String path = PERSON + "." + NAME;
            CaseField nestedElementByPath = debtorDetails.findNestedElementByPath(path);

            assertAll(
                () -> assertThat(nestedElementByPath.getId(), is(name.getId())),
                () -> assertThat(nestedElementByPath.getFieldType().getType(), is(name.getFieldType().getType())),
                () -> assertThat(nestedElementByPath.getFieldType().getId(), is(name.getFieldType().getId())),
                () -> assertThat(nestedElementByPath.getFieldType().getChildren().size(), is(0))
                     );
        }

        @Test
        void findNestedElementForCaseFieldWithNoNestedElements() {
            Exception exception = assertThrows(BadRequestException.class, () ->
                name.findNestedElementByPath("Field1"));
            assertEquals("CaseField Name has no nested elements.", exception.getMessage());
        }

        @Test
        void findNestedElementForCaseFieldWithNonMatchingPathElement() {
            Exception exception = assertThrows(BadRequestException.class, () ->
                debtorDetails.findNestedElementByPath("Field1"));
            assertEquals("Nested element not found for Field1", exception.getMessage());
        }

        @Test
        void findNestedElementForCaseFieldWithNonMatchingPathElements() {
            Exception exception = assertThrows(BadRequestException.class, () ->
                debtorDetails.findNestedElementByPath("Field2.Field3"));
            assertEquals("Nested element not found for Field2", exception.getMessage());
        }
    }

}
