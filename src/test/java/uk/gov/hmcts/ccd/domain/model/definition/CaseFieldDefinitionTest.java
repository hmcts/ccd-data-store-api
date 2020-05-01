package uk.gov.hmcts.ccd.domain.model.definition;

import java.util.List;
import java.util.Optional;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.ComplexACLBuilder.aComplexACL;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

import org.hamcrest.MatcherAssert;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameter;
import uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterType;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.List;
import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CaseFieldDefinitionTest {

    private static final String TEXT_TYPE = "Text";
    private static final String FAMILY = "Family";
    private static final String MEMBERS = "Members";
    private static final String PERSON = "Person";
    private static final String DEBTOR_DETAILS = "Debtor details";
    private static final String ADDRES_LINE_1 = "Address line 1";
    private static final String ADDRES_LINE_2 = "Address line 2";
    private static final String ADDRES_LINE_3 = "Address line 3";
    private static final String POSTCODE = "Postcode";
    private static final String COUNTRY = "Country";
    private static final String ADDRESS = "Address";
    private static final String NAME = "Name";
    private static final String SURNAME = "Surname";
    private static final String FAMILY_NAME = "Family Name";
    private static final String FAMILY_NAMES = "Family Names";
    private static final String FAMILY_INFO = "Family Info";
    private static final String FAMILY_ADDRESS = "Family Address";
    private static final String ROLE1 = "role1";
    private static final String ROLE2 = "role2";
    private static final String ROLE3 = "role3";

    private CaseFieldDefinition addressLine1 = newCaseField()
        .withId(ADDRES_LINE_1)
        .withFieldType(aFieldType()
            .withId(TEXT_TYPE)
            .withType(TEXT_TYPE)
            .build())
        .build();
    private CaseFieldDefinition addressLine2 = newCaseField()
        .withId(ADDRES_LINE_2)
        .withFieldType(aFieldType()
            .withId(TEXT_TYPE)
            .withType(TEXT_TYPE)
            .build())
        .build();
    private CaseFieldDefinition addressLine3 = newCaseField()
        .withId(ADDRES_LINE_3)
        .withFieldType(aFieldType()
            .withId(TEXT_TYPE)
            .withType(TEXT_TYPE)
            .build())
        .build();
    private CaseFieldDefinition postcode = newCaseField().withId(POSTCODE).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private CaseFieldDefinition country = newCaseField().withId(COUNTRY).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private FieldTypeDefinition addressFieldTypeDefinition = aFieldType()
        .withId(ADDRESS)
        .withType(COMPLEX)
        .withComplexField(addressLine1)
        .withComplexField(addressLine2)
        .withComplexField(addressLine3)
        .withComplexField(postcode)
        .withComplexField(country)
        .build();
    private CaseFieldDefinition address = newCaseField().withId(ADDRESS).withFieldType(addressFieldTypeDefinition).build();

    private CaseFieldDefinition name = newCaseField().withId(NAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private CaseFieldDefinition surname = newCaseField().withId(SURNAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private FieldTypeDefinition personFieldTypeDefinition = aFieldType()
        .withId(PERSON)
        .withType(COMPLEX)
        .withComplexField(name)
        .withComplexField(surname)
        .withComplexField(address)
        .build();
    private CaseFieldDefinition person = newCaseField().withId(PERSON).withFieldType(personFieldTypeDefinition).build();

    private FieldTypeDefinition debtorFieldTypeDefinition = aFieldType().withId(DEBTOR_DETAILS).withType(COMPLEX).withComplexField(person).build();
    private CaseFieldDefinition debtorDetails = newCaseField().withId(DEBTOR_DETAILS).withFieldType(debtorFieldTypeDefinition).build();

    private FieldTypeDefinition membersFieldTypeDefinition = aFieldType()
        .withId(MEMBERS + "-some-uid-value")
        .withType(COLLECTION)
        .withCollectionField(person)
        .build();
    private CaseFieldDefinition members = newCaseField().withId(MEMBERS).withFieldType(membersFieldTypeDefinition).build();

    private CaseFieldDefinition familyName = newCaseField()
        .withId(FAMILY_NAME)
        .withFieldType(aFieldType()
            .withId(TEXT_TYPE)
            .withType(TEXT_TYPE)
            .build())
        .build();
    private FieldTypeDefinition familyNamesFieldTypeDefinition = aFieldType().withId(FAMILY_NAMES).withType(COLLECTION).withCollectionField(familyName).build();
    private CaseFieldDefinition familyNames = newCaseField().withId(FAMILY_NAMES).withFieldType(familyNamesFieldTypeDefinition).build();


    private CaseFieldDefinition addressLine21 = newCaseField()
        .withId(ADDRES_LINE_1)
        .withFieldType(aFieldType()
            .withId(TEXT_TYPE)
            .withType(TEXT_TYPE)
            .build())
        .build();
    private CaseFieldDefinition addressLine22 = newCaseField()
        .withId(ADDRES_LINE_2)
        .withFieldType(aFieldType()
            .withId(TEXT_TYPE)
            .withType(TEXT_TYPE)
            .build())
        .build();
    private CaseFieldDefinition addressLine23 = newCaseField()
        .withId(ADDRES_LINE_3)
        .withFieldType(aFieldType()
            .withId(TEXT_TYPE)
            .withType(TEXT_TYPE)
            .build())
        .build();
    private CaseFieldDefinition postcode2 = newCaseField().withId(POSTCODE).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private CaseFieldDefinition country2 = newCaseField().withId(COUNTRY).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private FieldTypeDefinition addressFieldTypeDefinition2 = aFieldType()
        .withId(ADDRESS)
        .withType(COMPLEX)
        .withComplexField(addressLine21)
        .withComplexField(addressLine22)
        .withComplexField(addressLine23)
        .withComplexField(postcode2)
        .withComplexField(country2)
        .build();
    private CaseFieldDefinition familyAddress = newCaseField().withId(FAMILY_ADDRESS).withFieldType(addressFieldTypeDefinition2).build();
    private FieldTypeDefinition familyInfoType = aFieldType().withId(FAMILY_INFO)
        .withType(COMPLEX)
        .withComplexField(familyNames)
        .withComplexField(familyAddress)
        .build();
    private CaseFieldDefinition familyInfo = newCaseField().withId(FAMILY_INFO).withFieldType(familyInfoType).build();

    private FieldTypeDefinition familyFieldTypeDefinition =
        aFieldType().withId(FAMILY).withType(COMPLEX).withComplexField(familyInfo).withComplexField(members).build();
    private AccessControlList acl1 = anAcl().withRole(ROLE1).withCreate(true).withRead(true).withUpdate(true).withDelete(false).build();
    private AccessControlList acl2 = anAcl().withRole(ROLE2).withCreate(true).withRead(true).withUpdate(false).withDelete(true).build();
    private AccessControlList acl3 = anAcl().withRole(ROLE3).withCreate(false).withRead(false).withUpdate(true).withDelete(false).build();
    private ComplexACL complexACL1
        = aComplexACL().withListElementCode(MEMBERS).withRole(ROLE1).withCreate(false).withRead(true).withUpdate(true).withDelete(false).build();
    private ComplexACL complexACL2
        = aComplexACL().withListElementCode(MEMBERS + "." + PERSON).withRole(ROLE1).withCreate(false).withRead(true).withUpdate(false).withDelete(false)
            .build();
    private ComplexACL complexACL3
        = aComplexACL().withListElementCode(MEMBERS + "." + PERSON + "." + NAME).withRole(ROLE1).withCreate(false).withRead(true).withUpdate(false)
            .withDelete(false).build();
    private ComplexACL complexACL4
        = aComplexACL().withListElementCode(FAMILY_INFO).withRole(ROLE1).withCreate(true).withRead(true).withUpdate(true).withDelete(false).build();
    private ComplexACL complexACL5
        = aComplexACL().withListElementCode(FAMILY_INFO + "." + FAMILY_ADDRESS).withRole(ROLE1).withCreate(true).withRead(true).withUpdate(false)
            .withDelete(false).build();
    private CaseFieldDefinition family;

    @BeforeEach
    void setup() {
        family = newCaseField().withId(FAMILY).withFieldType(familyFieldTypeDefinition)
            .withComplexACL(complexACL1)
            .withComplexACL(complexACL4)
            .withComplexACL(complexACL5)
            .withAcl(acl1).withAcl(acl2).withAcl(acl3).build();
    }

    @Nested
    @DisplayName("propagateACLsToNestedFields test")
    class CaseFieldDefinitionSetChildrenACLsTest {

        @Test
        void propagateACLsToNestedFields() {

            family.propagateACLsToNestedFields();

            final CaseFieldDefinition members = family.getFieldTypeDefinition().getChildren().stream()
                .filter(e -> e.getId().equals(MEMBERS)).findFirst().get();
            final CaseFieldDefinition person = members.getFieldTypeDefinition().getChildren().stream()
                .filter(e -> e.getId().equals(PERSON)).findFirst().get();

            final CaseFieldDefinition name = person.getFieldTypeDefinition().getChildren().stream()
                .filter(e -> e.getId().equals(NAME)).findFirst().get();
            final CaseFieldDefinition surname = person.getFieldTypeDefinition().getChildren().stream()
                .filter(e -> e.getId().equals(SURNAME)).findFirst().get();

            validateACL(findNestedField(family, MEMBERS).getAccessControlLists(), complexACL1);
            validateACL(findNestedField(family, MEMBERS + "." + PERSON).getAccessControlLists(), complexACL1);
            validateACL(findNestedField(family, MEMBERS + "." + PERSON + "." + NAME).getAccessControlLists(), complexACL1);
            validateACL(findNestedField(family, MEMBERS + "." + PERSON + "." + SURNAME).getAccessControlLists(), complexACL1);
            validateACL(findNestedField(family, FAMILY_INFO + "." + FAMILY_ADDRESS + "." + ADDRES_LINE_1).getAccessControlLists(), complexACL5);
            validateACL(findNestedField(family, FAMILY_INFO + "." + FAMILY_ADDRESS + "." + ADDRES_LINE_2).getAccessControlLists(), complexACL5);

            assertAll(
                () -> assertThat(family.getAccessControlLists().size(), is(3)),
                () -> assertThat(familyNames.getAccessControlLists().size(), is(2)),
                () -> assertThat(findNestedField(family, FAMILY_INFO + "." + FAMILY_NAMES).getAccessControlLists(),
                    hasItems(
                        hasProperty("role", CoreMatchers.is(ROLE2)),
                        hasProperty("role", is(ROLE3)))),
                () -> assertThat(familyName.getAccessControlLists().size(), is(2)),
                () -> assertThat(findNestedField(family, FAMILY_INFO + "." + FAMILY_NAMES + "." + FAMILY_NAME).getAccessControlLists(),
                    hasItems(
                        hasProperty("role", CoreMatchers.is(ROLE2)),
                        hasProperty("role", is(ROLE3)))),
                () -> assertThat(members.getAccessControlLists().size(), is(3)),
                () -> assertThat(person.getAccessControlLists().size(), is(3)),
                () -> assertThat(name.getAccessControlLists().size(), is(3)),
                () -> assertThat(surname.getAccessControlLists().size(), is(3)));
        }

        @Test
        @DisplayName("should clear ACLs for siblings not defined in ComplexACLs")
        void siblingsWithMissingComplexACLsMustBeCleared() {
            family.getComplexACLs().add(complexACL2);
            family.getComplexACLs().add(complexACL3);

            family.propagateACLsToNestedFields();

            validateACL(findNestedField(family, MEMBERS).getAccessControlLists(), complexACL1);
            validateACL(findNestedField(family, MEMBERS + "." + PERSON).getAccessControlLists(), complexACL2);
            validateACL(findNestedField(family, MEMBERS + "." + PERSON + "." + NAME).getAccessControlLists(), complexACL3);
            assertThat(((CaseFieldDefinition) findNestedField(family, MEMBERS + "." + PERSON + "." + SURNAME))
                .getAccessControlListByRole(ROLE1), is(Optional.empty()));
            assertThrows(RuntimeException.class, () -> findNestedField(family, MEMBERS + "." + PERSON + "." + SURNAME).getAccessControlLists().stream()
                .filter(el -> el.getRole().equalsIgnoreCase(ROLE1))
                .findFirst()
                .orElseThrow(RuntimeException::new));
        }
    }

    @Nested
    @DisplayName("find by path tests")
    class FindNestedElementsTest {

        @Test
        void findNestedElementByPath() {
            String path = PERSON + "." + NAME;
            CaseFieldDefinition nestedElementByPath = (CaseFieldDefinition) findNestedField(debtorDetails, path);

            assertAll(
                () -> assertThat(nestedElementByPath.getId(), is(name.getId())),
                () -> assertThat(nestedElementByPath.getFieldTypeDefinition().getType(), is(name.getFieldTypeDefinition().getType())),
                () -> assertThat(nestedElementByPath.getFieldTypeDefinition().getId(), is(name.getFieldTypeDefinition().getId())),
                () -> assertThat(nestedElementByPath.getFieldTypeDefinition().getChildren().size(), is(0)));
        }

        @Test
        void findNestedElementForCaseFieldWithEmptyPath() {
            CaseFieldDefinition nestedElementByPath = (CaseFieldDefinition) findNestedField(debtorDetails,"");
            assertEquals(debtorDetails, nestedElementByPath);
        }

        @Test
        void findNestedElementForCaseFieldWithNullPath() {
            CaseFieldDefinition nestedElementByPath = (CaseFieldDefinition) findNestedField(debtorDetails,null);
            assertEquals(debtorDetails, nestedElementByPath);
        }

        @Test
        void findNestedElementForCaseFieldWithNoNestedElements() {
            Exception exception = assertThrows(BadRequestException.class, () ->
                findNestedField(name,"Field1"));
            assertEquals("CaseViewField " + NAME + " has no nested elements with code Field1.", exception.getMessage());
        }

        @Test
        void findNestedElementForCaseFieldWithNonMatchingPathElement() {
            Exception exception = assertThrows(BadRequestException.class, () ->
                findNestedField(debtorDetails,"Field1"));
            assertEquals("CaseViewField " + DEBTOR_DETAILS + " has no nested elements with code Field1.", exception.getMessage());
        }

        @Test
        void findNestedElementForCaseFieldWithNonMatchingPathElements() {
            Exception exception = assertThrows(BadRequestException.class, () ->
                findNestedField(debtorDetails,"Field2.Field3"));
            assertEquals("CaseViewField " + DEBTOR_DETAILS + " has no nested elements with code Field2.Field3.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("displayContextType test")
    class CaseFieldDisplayContextTypeTest {

        @Test
        void shouldReturnCorrectDisplayContextEnumForReadOnlyContext() {
            name.setDisplayContext("READONLY");

            DisplayContext result = name.displayContextType();

            MatcherAssert.assertThat(result, is(DisplayContext.READONLY));
        }

        @Test
        void shouldReturnCorrectDisplayContextEnumForMandatoryContext() {
            name.setDisplayContext("MANDATORY");

            DisplayContext result = name.displayContextType();

            MatcherAssert.assertThat(result, is(DisplayContext.MANDATORY));
        }

        @Test
        void shouldReturnCorrectDisplayContextEnumForOptionalContext() {
            name.setDisplayContext("OPTIONAL");

            DisplayContext result = name.displayContextType();

            MatcherAssert.assertThat(result, is(DisplayContext.OPTIONAL));
        }

        @Test
        void shouldReturnNullForHiddenContext() {
            name.setDisplayContext("HIDDEN");

            DisplayContext result = name.displayContextType();

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("displayContextParameter tests")
    class CaseFieldDisplayContextParameterTest {

        @Test
        void shouldReturnListOfDCPWithSingleDCP() {
            name.setDisplayContextParameter("#DATETIMEENTRY(ddMMyy)");

            final List<DisplayContextParameter> result = name.getDisplayContextParameters();

            assertAll(
                () -> assertThat(result.size(), is(1)),
                () -> assertThat(result.get(0).getType(), is(DisplayContextParameterType.DATETIMEENTRY)),
                () -> assertThat(result.get(0).getValue(), is("ddMMyy"))
            );
        }

        @Test
        void shouldReturnListOfDCPWithMultipleDCP() {
            name.setDisplayContextParameter("#DATETIMEENTRY(ddMMyy),#DATETIMEDISPLAY(yyyy)");

            final List<DisplayContextParameter> result = name.getDisplayContextParameters();

            assertAll(
                () -> assertThat(result.size(), is(2)),
                () -> assertThat(result.get(0).getType(), is(DisplayContextParameterType.DATETIMEENTRY)),
                () -> assertThat(result.get(0).getValue(), is("ddMMyy")),
                () -> assertThat(result.get(1).getType(), is(DisplayContextParameterType.DATETIMEDISPLAY)),
                () -> assertThat(result.get(1).getValue(), is("yyyy"))
            );
        }

        @Test
        void shouldNotIncludeInvalidTypeOfDCP() {
            name.setDisplayContextParameter("#INVALID(ddMMyy),#DATETIMEDISPLAY(yyyy)");

            final List<DisplayContextParameter> result = name.getDisplayContextParameters();

            assertAll(
                () -> assertThat(result.size(), is(1)),
                () -> assertThat(result.get(0).getType(), is(DisplayContextParameterType.DATETIMEDISPLAY)),
                () -> assertThat(result.get(0).getValue(), is("yyyy"))
            );
        }

        @Test
        void shouldNotIncludeEmptyValueOfDCP() {
            name.setDisplayContextParameter("#DATETIMEENTRY(),#DATETIMEDISPLAY(yyyy)");

            final List<DisplayContextParameter> result = name.getDisplayContextParameters();

            assertAll(
                () -> assertThat(result.size(), is(1)),
                () -> assertThat(result.get(0).getType(), is(DisplayContextParameterType.DATETIMEDISPLAY)),
                () -> assertThat(result.get(0).getValue(), is("yyyy"))
            );
        }

        @Test
        void shouldReturnEmptyListWithNoDCP() {
            name.setDisplayContextParameter(null);

            final List<DisplayContextParameter> result = name.getDisplayContextParameters();

            assertAll(
                () -> assertThat(result.size(), is(0))
            );
        }

        @Test
        void shouldReturnDCPOfSpecifiedType() {
            name.setDisplayContextParameter("#DATETIMEENTRY(dd),#DATETIMEDISPLAY(yyyy)");

            final Optional<DisplayContextParameter> result = name.getDisplayContextParameter(DisplayContextParameterType.DATETIMEDISPLAY);

            assertAll(
                () -> assertThat(result.isPresent(), is(true)),
                () -> assertThat(result.get().getType(), is(DisplayContextParameterType.DATETIMEDISPLAY)),
                () -> assertThat(result.get().getValue(), is("yyyy"))
            );
        }

        @Test
        void shouldReturnEmptyOptionalWhenDCPOfSpecifiedTypeDoesNotExist() {
            name.setDisplayContextParameter("#DATETIMEENTRY(dd),#DATETIMEDISPLAY(yyyy)");

            final Optional<DisplayContextParameter> result = name.getDisplayContextParameter(DisplayContextParameterType.LIST);

            assertAll(
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        void shouldReturnTrueWhenDCPTypeIsPresent() {
            name.setDisplayContextParameter("#DATETIMEENTRY(ddMMyy)");

            final boolean result = name.hasDisplayContextParameter(DisplayContextParameterType.DATETIMEENTRY);

            assertAll(
                () -> assertThat(result, is(true))
            );
        }

        @Test
        void shouldReturnFalseWhenDCPTypeIsNotPresent() {
            name.setDisplayContextParameter("#DATETIMEENTRY(ddMMyy)");

            final boolean result = name.hasDisplayContextParameter(DisplayContextParameterType.DATETIMEDISPLAY);

            assertAll(
                () -> assertThat(result, is(false))
            );
        }
    }

    void validateACL(List<AccessControlList> acls, AccessControlList expected) {
        final AccessControlList acl1 = acls.stream()
            .filter(el -> el.getRole().equalsIgnoreCase(expected.getRole()))
            .findFirst()
            .orElseThrow(RuntimeException::new);
        assertAll(
            () -> assertThat("Create doesn't match", acl1.isCreate(), is(expected.isCreate())),
            () -> assertThat("Read doesn't match", acl1.isRead(), is(expected.isRead())),
            () -> assertThat("Update doesn't match", acl1.isUpdate(), is(expected.isUpdate())),
            () -> assertThat("Delete doesn't match", acl1.isDelete(), is(expected.isDelete()))
        );
    }

    public static CommonField findNestedField(final CommonField caseViewField, final String childFieldId) {
        return caseViewField.getComplexFieldNestedField(childFieldId)
            .orElseThrow(() -> new BadRequestException(format("CaseViewField %s has no nested elements with code %s.", caseViewField.getId(), childFieldId)));
    }
}
