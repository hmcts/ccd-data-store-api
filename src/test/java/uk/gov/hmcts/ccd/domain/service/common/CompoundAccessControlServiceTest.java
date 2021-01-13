package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.io.IOException;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.ROLE_IN_USER_ROLES;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.USER_ROLES;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.addressesStart;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.getPeopleCollectionFieldDefinition;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.getTagFieldDefinition;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.p2Address1;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.p2Address2;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.p2End;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.p2Names;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.p2Notes;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.p2Start;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.person1;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.person2;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.ComplexACLBuilder.aComplexACL;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class CompoundAccessControlServiceTest {
    private static final ObjectMapper MAPPER = JacksonUtils.MAPPER;
    private CompoundAccessControlService compoundAccessControlService;
    private static final String newAddress1 = "      {\n"
        + "        \"value\": {\n"
        + "           \"Name\": \"home\",\n"
        + "           \"Address\": {"
        + "               \"Line1\": \"231 Clampton Road\",\n"
        + "               \"Line2\": \"Fitzgrovia, London\",\n"
        + "               \"PostCode\": \"EC2 5GN\",\n"
        + "               \"Country\": \"United Kingdom\"\n"
        + "           }\n"
        + "        },\n"
        + "        \"id\": \"null\"\n"
        + "      }\n";
    private static final String existingAddress1 = "      {\n"
        + "        \"value\": {\n"
        + "           \"Name\": \"home\",\n"
        + "           \"Address\": {"
        + "               \"Line1\": \"666 Clampton Road\",\n"
        + "               \"Line2\": \"Fitzgrovia, London\",\n"
        + "               \"PostCode\": \"EC2 5GN\",\n"
        + "               \"Country\": \"United Kingdom\"\n"
        + "           }\n"
        + "        },\n"
        + "        \"id\": \"2939423847298729384\"\n"
        + "      }\n";
    private static final String existingAddress1NullLine1 = "      {\n"
        + "        \"value\": {\n"
        + "           \"Name\": \"home\",\n"
        + "           \"Address\": {"
        + "               \"Line2\": \"Fitzgrovia, London\",\n"
        + "               \"PostCode\": \"EC2 5GN\",\n"
        + "               \"Country\": \"United Kingdom\"\n"
        + "           }\n"
        + "        },\n"
        + "        \"id\": \"2939423847298729384\"\n"
        + "      }\n";
    private static final String existingAddressWMissingLines = "      {\n"
        + "        \"value\": {\n"
        + "           \"Name\": \"home\",\n"
        + "           \"Address\": {"
        + "               \"Country\": \"United Kingdom\"\n"
        + "           }\n"
        + "        },\n"
        + "        \"id\": \"2939423847298729384\"\n"
        + "      }\n";
    private static final String existingAddressWNullLines = "      {\n"
        + "        \"value\": {\n"
        + "           \"Name\": \"home\",\n"
        + "           \"Address\": {"
        + "               \"Line1\": null,\n"
        + "               \"Line2\": null,\n"
        + "               \"Country\": \"United Kingdom\"\n"
        + "           }\n"
        + "        },\n"
        + "        \"id\": \"2939423847298729384\"\n"
        + "      }\n";
    private static final String existingAddress1Line1Updated = "      {\n"
        + "        \"value\": {\n"
        + "           \"Name\": \"home\",\n"
        + "           \"Address\": {"
        + "               \"Line1\": \"XYZ Clampton Road\",\n"
        + "               \"Line2\": \"Fitzgrovia, London\",\n"
        + "               \"PostCode\": \"EC2 5GN\",\n"
        + "               \"Country\": \"United Kingdom\"\n"
        + "           }\n"
        + "        },\n"
        + "        \"id\": \"2939423847298729384\"\n"
        + "      }\n";
    private static final String existingAddress1LinesUpdated = "      {\n"
        + "        \"value\": {\n"
        + "           \"Name\": \"homerton\",\n"
        + "           \"Address\": {"
        + "               \"Line1\": \"ABC Clampton Road\",\n"
        + "               \"Line2\": \"Belgravia, London\",\n"
        + "               \"PostCode\": \"EC2 5GN\",\n"
        + "               \"Country\": \"United Kingdom\"\n"
        + "           }\n"
        + "        },\n"
        + "        \"id\": \"2939423847298729384\"\n"
        + "      }\n";
    private static final String existingAddress2 = "      {\n"
        + "        \"value\": {\n"
        + "           \"Name\": \"home\",\n"
        + "           \"Address\": {"
        + "               \"Line1\": \"101 Humanities Road\",\n"
        + "               \"Line2\": \"Fitzgrovia, London\",\n"
        + "               \"PostCode\": \"EC2 5GN\",\n"
        + "               \"Country\": \"United Kingdom\"\n"
        + "           }\n"
        + "        },\n"
        + "        \"id\": \"2939423847298729399\"\n"
        + "      }\n";
    private static final String newAddress2 = "      {\n"
        + "        \"value\": {\n"
        + "          \"Name\": \"home\",\n"
        + "           \"Address\": {"
        + "               \"Line1\": \"1 Hampton Road\",\n"
        + "               \"Line2\": \"London\",\n"
        + "               \"PostCode\": \"EC5 5GN\",\n"
        + "               \"Country\": \"United Kingdom\"\n"
        + "           }\n"
        + "        },\n"
        + "        \"id\": \"null\"\n"
        + "      }\n";
    private static final String newAddress3 = "      {\n"
        + "        \"value\": {\n"
        + "           \"Name\": \"work\",\n"
        + "           \"Address\": {"
        + "               \"Line1\": \"17 Prune Road\",\n"
        + "               \"Line2\": \"London\",\n"
        + "               \"PostCode\": \"EC5 5GN\",\n"
        + "               \"Country\": \"United Kingdom\"\n"
        + "           }\n"
        + "        },\n"
        + "        \"id\": \"null\"\n"
        + "      }\n";
    private static final String addressEnd = "    ]\n";
    private static final String addresses =
        addressesStart + newAddress1 + "," + newAddress2 + "," + newAddress3 + addressEnd;
    private static final String notes = "    \"Notes\": [\n"
        + "      {\n"
        + "        \"value\": {\n"
        + "          \"Txt\": \"someNote11\",\n"
        + "           \"Tags\": [\n"
        + "               {\n"
        + "                   \"value\": {\n"
        + "                       \"Tag\": \"private\",\n"
        + "                       \"Category\": \"Personal\"\n"
        + "                   },\n"
        + "                   \"id\": \"null\"\n"
        + "               }\n"
        + "           ]\n"
        + "        },\n"
        + "        \"id\": \"null\"\n"
        + "      },\n"
        + "      {\n"
        + "        \"value\": {\n"
        + "          \"Txt\": \"someNote21\"\n"
        + "        },\n"
        + "        \"id\": \"null\"\n"
        + "      }\n"
        + "    ]\n";
    private static final String notesWId = "    \"Notes\": [\n"
        + "      {\n"
        + "        \"value\": {\n"
        + "          \"Txt\": \"someNote11\",\n"
        + "           \"Tags\": [\n"
        + "               {\n"
        + "                   \"value\": {\n"
        + "                       \"Tag\": \"private\",\n"
        + "                       \"Category\": \"Personal\"\n"
        + "                   },\n"
        + "                   \"id\": \"2342342345\"\n"
        + "               }\n"
        + "           ]\n"
        + "        },\n"
        + "        \"id\": \"456334563456\"\n"
        + "      },\n"
        + "      {\n"
        + "        \"value\": {\n"
        + "          \"Txt\": \"someNote21\"\n"
        + "        },\n"
        + "        \"id\": \"234234234\"\n"
        + "      }\n"
        + "    ]\n";
    private static final String notesWIdDeletedTags = "    \"Notes\": [\n"
        + "      {\n"
        + "        \"value\": {\n"
        + "          \"Txt\": \"someNote11\"\n"
        + "        },\n"
        + "        \"id\": \"456334563456\"\n"
        + "      },\n"
        + "      {\n"
        + "        \"value\": {\n"
        + "          \"Txt\": \"someNote21\"\n"
        + "        },\n"
        + "        \"id\": \"234234234\"\n"
        + "      }\n"
        + "    ]\n";
    private static final String notesWIdWNewlyAddedTags = "    \"Notes\": [\n"
        + "      {\n"
        + "        \"value\": {\n"
        + "          \"Txt\": \"someNote11\",\n"
        + "           \"Tags\": [\n"
        + "               {\n"
        + "                   \"value\": {\n"
        + "                       \"Tag\": \"private\",\n"
        + "                       \"Category\": \"Personal\"\n"
        + "                   },\n"
        + "                   \"id\": \"2342342345\"\n"
        + "               }\n"
        + "           ]\n"
        + "        },\n"
        + "        \"id\": \"456334563456\"\n"
        + "      },\n"
        + "      {\n"
        + "        \"value\": {\n"
        + "          \"Txt\": \"someNote21\",\n"
        + "           \"Tags\": [\n"
        + "               {\n"
        + "                   \"value\": {\n"
        + "                       \"Tag\": \"private\",\n"
        + "                       \"Category\": \"Personal\"\n"
        + "                   },\n"
        + "                   \"id\": \"null\"\n"
        + "               },\n"
        + "               {\n"
        + "                   \"value\": {\n"
        + "                       \"Tag\": \"private\",\n"
        + "                       \"Category\": \"Personal\"\n"
        + "                   },\n"
        + "                   \"id\": \"null\"\n"
        + "               },\n"
        + "               {\n"
        + "                   \"value\": {\n"
        + "                       \"Tag\": \"private\",\n"
        + "                       \"Category\": \"Personal\"\n"
        + "                   },\n"
        + "                   \"id\": \"null\"\n"
        + "               }\n"
        + "           ]\n"
        + "        },\n"
        + "        \"id\": \"234234234\"\n"
        + "      }\n"
        + "    ]\n";
    private static final String birthInfo = "    \"BirthInfo\": {\n"
        + "         \"BornCity\": \"Madrid\",\n"
        + "         \"BornCountry\": \"Spain\",\n"
        + "         \"BornAddress\": {\n"
        + "               \"Name\": \"holiday\",\n"
        + "               \"Address\": {"
        + "                   \"Line1\": \"118 Chapman Lane\",\n"
        + "                   \"Line2\": \"London\",\n"
        + "                   \"PostCode\": \"EC5 5GN\",\n"
        + "                   \"Country\": \"United Kingdom\"\n"
        + "               }\n"
        + "         }\n"
        + "     }\n";
    private static final String newPersonStart = "{\n"
        + "  \"id\": \"null\",\n"
        + "  \"value\": {\n";
    private static final String existingPersonStart = "{\n"
        + "  \"id\": \"273647284\",\n"
        + "  \"value\": {\n";
    private static final String name = "    \"FirstName\": \"Harry\",\n"
        + "    \"LastName\": \"Potter\"\n";
    private static final String nameUpdated = "    \"FirstName\": \"Harry Junior\",\n"
        + "    \"LastName\": \"Potter\"\n";
    private static final String personEnd = "  }\n"
        + "}";
    private static final String newPerson =
        newPersonStart + name + "," + birthInfo + "," + addresses + "," + notes + personEnd;
    private static final String peopleStart = "{\n  \"People\": [\n";
    private static final String peopleEnd = "  ]\n}";

    @BeforeEach
    void setup() {
        compoundAccessControlService = new CompoundAccessControlService();
    }

    @Nested
    @DisplayName("Compound Field - Create Tests")
    class CompoundFieldCreateTests {

        @Test
        @DisplayName("Should grant access if parent and children have ACLs")
        void shouldGrantAccessIfParentAndChildrenHaveAccess() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleDataWithPerson(person1, person2);

            assertThat(
                compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(person1, person2,
                    newPerson), dataNode, people, USER_ROLES),
                is(true)
            );
        }

        @Test
        @DisplayName("Should grant access if parent and children have ACLs - inherited from parent")
        void shouldGrantAccessIfParentHasAccess() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleDataWithPerson(person1, person2);

            assertThat(compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(person1,
                person2, newPersonStart + addresses + personEnd),
                dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should be OK with empty fields")
        void shouldBeOKWithEmptyFields() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleDataWithPerson(person1, person2);

            assertThat(compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(person1,
                person2, newPersonStart + personEnd),
                dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should grant access if parent and required children have ACLs")
        void shouldGrantAccessIfParentAndRequiredChildrenHaveAccess() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(false)
                    .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleDataWithPerson(person1, person2);

            assertThat(compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(person1,
                person2, newPersonStart + name + "," + birthInfo + "," + addresses + personEnd), dataNode, people,
                USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should grant access to add new child if child has the required ACLs - existing data")
        void shouldGrantAccessToNewChildIfChildrenHasAccess() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));
            people.setComplexACLs(asList(aComplexACL()
                .withListElementCode("Addresses")
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String person = p2Start + addressesStart + p2Address1 + "," + p2Address2 + addressEnd + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(person);

            String personWithNewAddress =
                p2Start + addressesStart + p2Address1 + "," + p2Address2 + "," + newAddress1 + addressEnd + p2End;
            assertThat(
                compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(personWithNewAddress),
                    dataNode, people, USER_ROLES),
                is(true)
            );
        }

        @Test
        @DisplayName("Should grant access to add new child if child has the required ACLs - fine grain ACL")
        void shouldGrantAccessToNewChildIfChildrenHasAccessFineGrained() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));
            people.setComplexACLs(asList(aComplexACL()
                .withListElementCode("Notes.Tags")
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String person =
                p2Start + addressesStart + p2Address1 + "," + p2Address2 + addressEnd + "," + notesWId + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(person);

            String personWithNewAddress = p2Start + addressesStart + p2Address1 + "," + p2Address2 + addressEnd + ","
                + notesWIdWNewlyAddedTags + p2End;
            assertThat(
                compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(personWithNewAddress),
                    dataNode, people, USER_ROLES),
                is(true)
            );
        }

        @Test
        @DisplayName("Should deny access to add new child if child has the required ACLs - fine grain ACL")
        void shouldDenyAccessToNewChildIfChildrenHasAccessFineGrained() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));
            people.setComplexACLs(asList(aComplexACL()
                .withListElementCode("Notes.Tags")
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(false)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String person =
                p2Start + addressesStart + p2Address1 + "," + p2Address2 + addressEnd + "," + notesWId + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(person);

            String personWithNewAddress = p2Start + addressesStart + p2Address1 + "," + p2Address2 + addressEnd + ","
                + notesWIdWNewlyAddedTags + p2End;
            assertThat(
                compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(personWithNewAddress),
                    dataNode, people, USER_ROLES),
                is(false)
            );
        }

        @Test
        @DisplayName("Should deny access to add new child if child lacks the required ACLs - existing data")
        void shouldDenyAccessToNewChildIfChildrenHasAccess() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));
            people.setComplexACLs(asList(aComplexACL()
                .withListElementCode("Addresses")
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(false)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String person = p2Start + addressesStart + p2Address1 + "," + p2Address2 + addressEnd + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(person);

            String personWithNewAddress = p2Start + addressesStart + p2Address1 + "," + p2Address2 + "," + newAddress1
                + addressEnd + p2End;
            assertThat(
                compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(personWithNewAddress),
                    dataNode, people, USER_ROLES),
                is(false)
            );
        }

        @Test
        @DisplayName("Should deny access if a child does not have ACLs")
        void shouldDenyAccessIfParentAndChildrenHaveAccess() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleDataWithPerson(person1, person2);

            assertThat(
                compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(person1, person2,
                    newPerson), dataNode, people, USER_ROLES),
                is(false)
            );
        }
    }

    @Nested
    @DisplayName("Compound Field - Update Tests")
    class CompoundFieldUpdateTests {
        @Test
        @DisplayName("Should grant access when nothing changes even when U doesn't exist")
        void shouldGrantAccessWhenNoUpdates() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withUpdate(false)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleDataWithPerson(person1);

            assertThat(compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(person1),
                dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should grant access when child field updated and U exists- name change")
        void shouldGrantAccessWhenChildFieldUpdatedAndACLExists() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withUpdate(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleDataWithPerson(existingPersonStart + name + personEnd);

            assertThat(compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(existingPersonStart
                    + nameUpdated + personEnd),
                dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should deny access for child field updates when no U - name change")
        void shouldDenyAccessWhenChildFieldUpdatedAndNoACL() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withUpdate(false)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleDataWithPerson(existingPersonStart + name + personEnd);

            assertThat(compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(existingPersonStart
                    + nameUpdated + personEnd),
                dataNode, people, USER_ROLES), is(false));
        }

        @Test
        @DisplayName("Should grant access when child field updated and U exists - address.line1 change")
        void shouldGrantAccessWhenChildFieldUpdatedAndACLInheritedFromParent() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withUpdate(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleDataWithPerson(existingPersonStart + addressesStart + existingAddress1
                + addressEnd + personEnd);

            assertThat(compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(existingPersonStart
                + addressesStart
                + existingAddress1Line1Updated + addressEnd + personEnd), dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should grant access when a child is updated and U exist - multiple address.line1 change")
        void shouldGrantAccessWhenAChildFieldUpdatedAndACLExist() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withUpdate(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + addressesStart + existingAddress1 + "," + existingAddress2 + addressEnd
                + personEnd;
            String p2 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + "," + existingAddress1
                + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(p1, p2);

            String p1Updated = existingPersonStart + addressesStart + existingAddress2 + ","
                + existingAddress1Line1Updated + addressEnd + personEnd;
            String p2Updated = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + ","
                + existingAddress1Line1Updated + addressEnd + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(p2Updated, p1Updated), dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should deny access when a child is updated and U doesn't exist - multiple address.line1 change")
        void shouldDenyAccessWhenChildUpdatedAndNoACL() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(aComplexACL()
                .withListElementCode("Addresses")
                .withRole(ROLE_IN_USER_ROLES)
                .withUpdate(false)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + addressesStart + existingAddress1 + "," + existingAddress2 + addressEnd
                + personEnd;
            String p2 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + "," + existingAddress1
                + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(p1, p2);

            String p1Updated = existingPersonStart + addressesStart + existingAddress2 + ","
                + existingAddress1Line1Updated + addressEnd + personEnd;
            String p2Updated = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + ","
                + existingAddress1Line1Updated + addressEnd + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(p2Updated, p1Updated), dataNode, people, USER_ROLES), is(false));
        }

        @Test
        @DisplayName("Should grant access when child nodes same but a new node added C exist and No U - new address "
            + "added")
        void shouldGrantAccessWhenChildNotUpdatedAndOnlyNewChildAdded() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .withUpdate(false)
                    .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p2 =
                p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(p2);

            String p2Updated = p2Start + p2Names + addressesStart + newAddress1 + "," + p2Address1 + "," + p2Address2
                + addressEnd + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(p2Updated),
                dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should grand access when a child is updated and U exist - fine grained ACL")
        void shouldGrantAccessWhenChildUpdatedAndFineGrainedACLExists() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build()
            ));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + addressesStart + existingAddress1 + "," + existingAddress2 + addressEnd
                + personEnd;
            String p2 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + "," + existingAddress1
                + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(p1, p2);

            String p1Updated = existingPersonStart + addressesStart + existingAddress2 + ","
                + existingAddress1LinesUpdated + addressEnd + personEnd;
            String p2Updated = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + ","
                + existingAddress1LinesUpdated + addressEnd
                + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(p2Updated, p1Updated), dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should grand access when a child is updated and U exist, complex child has no initial value - "
            + "fine grained ACL")
        void shouldGrantAccessWhenChildUpdatedFromNullAndFineGrainedACLExists() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build()
            ));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + addressesStart + existingAddress1NullLine1 + "," + existingAddress2
                + addressEnd + personEnd;
            String p2 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + "," + existingAddress1
                + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(p1, p2);

            String p1Updated = existingPersonStart + addressesStart + existingAddress2 + ","
                + existingAddress1LinesUpdated + addressEnd + personEnd;
            String p2Updated = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + ","
                + existingAddress1LinesUpdated + addressEnd + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(p2Updated, p1Updated), dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should grand access when a child is updated and U exist, complex child has null initial value - "
            + "fine grained ACL")
        void shouldGrantAccessWhenChildUpdatedFromNullNodeAndFineGrainedACLExists() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.PostCode")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Country")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build()
            ));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + addressesStart + existingAddressWNullLines + "," + existingAddress2
                + addressEnd + personEnd;
            String p2 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + "," + existingAddress1
                + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(p1, p2);

            String p1Updated = existingPersonStart + addressesStart + existingAddress2 + ","
                + existingAddress1LinesUpdated + addressEnd + personEnd;
            String p2Updated = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + ","
                + existingAddress1LinesUpdated + addressEnd + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(p2Updated, p1Updated), dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should grand access when a child is updated and U exist, complex child has null final value - "
            + "fine grained ACL")
        void shouldGrantAccessWhenChildUpdatedToNullAndFineGrainedACLExists() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build()
            ));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + addressesStart + existingAddress1 + "," + existingAddress2 + addressEnd
                + personEnd;
            String p2 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + "," + existingAddress1
                + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(p1, p2);

            String p1Updated = existingPersonStart + addressesStart + existingAddress2 + "," + existingAddress1NullLine1
                + addressEnd + personEnd;
            String p2Updated = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + ","
                + existingAddress1LinesUpdated + addressEnd + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(p2Updated, p1Updated), dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should deny access when a child is updated and U doesnot exist, complex child has no initial "
            + "value - fine grained ACL")
        void shouldDenyAccessWhenChildUpdatedFromNullAndFineGrainedACLDoesNotExist() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build()
            ));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + addressesStart + existingAddress1NullLine1 + "," + existingAddress2
                + addressEnd + personEnd;
            String p2 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + "," + existingAddress1
                + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(p1, p2);

            String p1Updated = existingPersonStart + addressesStart + existingAddress2 + ","
                + existingAddress1LinesUpdated + addressEnd + personEnd;
            String p2Updated = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + ","
                + existingAddress1LinesUpdated + addressEnd
                + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(p2Updated, p1Updated), dataNode, people, USER_ROLES), is(false));
        }

        @Test
        @DisplayName("Should deny access when a child is updated and U doesnot exist, complex child has null final "
            + "value - fine grained ACL")
        void shouldDenyAccessWhenChildUpdatedToNullAndFineGrainedACLDoesNotExist() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build()
            ));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + addressesStart + existingAddress1 + "," + existingAddress2 + addressEnd
                + personEnd;
            String p2 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + "," + existingAddress1
                + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(p1, p2);

            String p1Updated = existingPersonStart + addressesStart + existingAddress2 + "," + existingAddress1NullLine1
                + addressEnd + personEnd;
            String p2Updated = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + ","
                + existingAddress1LinesUpdated + addressEnd + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(p2Updated, p1Updated), dataNode, people, USER_ROLES), is(false));
        }

        @Test
        @DisplayName("Should grant access when child is not updated and No U exists but 'null' is sent as value - "
            + "READONLY case")
        void shouldGrantAccessWhenChildIsNotUpdatedAndNullValueSent() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(false)
                    .build()
            ));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + addressesStart + existingAddressWMissingLines + "," + existingAddress2
                + addressEnd + personEnd;
            JsonNode dataNode = generatePeopleDataWithPerson(p1);

            String p1Updated = existingPersonStart + addressesStart + existingAddressWNullLines + "," + existingAddress2
                + addressEnd + personEnd;
            assertThat(compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(p1Updated),
                dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should deny access when a child is updated and No U exist - fine grained ACL Name, address."
            + "line1/2 changes")
        void shouldDenyAccessWhenChildUpdatedAndNoFineGrainedACLExists() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(false)
                    .build()
            ));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + addressesStart + existingAddress1 + "," + existingAddress2 + addressEnd
                + personEnd;
            String p2 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + "," + existingAddress1
                + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(p1, p2);

            String p1Updated = existingPersonStart + addressesStart + existingAddress2 + ","
                + existingAddress1LinesUpdated + addressEnd + personEnd;
            String p2Updated = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + ","
                + existingAddress1LinesUpdated + addressEnd + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(p2Updated, p1Updated), dataNode, people, USER_ROLES), is(false));
        }
    }

    @Nested
    @DisplayName("Compound Field - Delete Tests")
    class CompoundFieldDeleteTests {
        @Test
        @DisplayName("Should grant access when a root node is deleted and D exists")
        void shouldGrantAccessWhenRootDeletedAndACLExist() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleDataWithPerson(person1, person2);

            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(person1), dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should deny access when a root node is deleted and No D")
        void shouldDenyAccessWhenRootDeletedAndNoACL() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(false)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleDataWithPerson(person1, person2);

            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(person1), dataNode, people, USER_ROLES), is(false));
        }

        @Test
        @DisplayName("Should grant access when a child node is deleted and D exists")
        void shouldGrantAccessWhenChildDeletedAndACLExist() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + "," + existingAddress1
                + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(p1);

            String p1Updated = p2Start + p2Names + addressesStart + p2Address1 + "," + existingAddress1 + addressEnd
                + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(p1Updated), dataNode, people, USER_ROLES), is(true));
        }

        @Test
        @DisplayName("Should deny access when a child node is deleted and No D")
        void shouldDenyAccessWhenChildDeletedAndNoACLExist() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(false)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p2 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + "," + existingAddress1
                + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(person1, p2);

            String p2Updated = p2Start + p2Names + addressesStart + p2Address1 + "," + existingAddress1 + addressEnd
                + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(p2Updated, person1), dataNode, people, USER_ROLES), is(false));
        }

        @Test
        @DisplayName("Should deny access when a child node is deleted and No D - fine grained ACL")
        void shouldDenyAccessWhenChildDeletedAndNoACLExistForChildField() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withDelete(false)
                    .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + "," + existingAddress1
                + addressEnd + "," + p2Notes + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(p1);

            String p1Updated = p2Start + p2Names + addressesStart + p2Address1 + "," + existingAddress1 + addressEnd
                + "," + p2Notes + p2End;
            assertThat(compoundAccessControlService.hasAccessForAction(
                generatePeopleDataWithPerson(p1Updated), dataNode, people, USER_ROLES), is(false));
        }

        @Test
        @DisplayName("Should grant access to add new child if child has the required ACLs - whole node deleted")
        void shouldGrantAccessToNewChildIfChildrenHasAccessFineGrained() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));
            people.setComplexACLs(asList(aComplexACL()
                .withListElementCode("Notes.Tags")
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String person = p2Start + addressesStart + p2Address1 + "," + p2Address2 + addressEnd + "," + notesWId
                + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(person);

            String personWithNewAddress = p2Start + addressesStart + p2Address1 + "," + p2Address2 + addressEnd + ","
                + notesWIdDeletedTags + p2End;
            assertThat(
                compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(personWithNewAddress),
                    dataNode, people, USER_ROLES),
                is(true)
            );
        }

        @Test
        @DisplayName("Should deny access to add new child if child has the required ACLs - whole node deleted")
        void shouldDenyAccessToNewChildIfChildrenHasAccessFineGrained() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));
            people.setComplexACLs(asList(aComplexACL()
                .withListElementCode("Notes.Tags")
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(false)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String person = p2Start + addressesStart + p2Address1 + "," + p2Address2 + addressEnd + "," + notesWId
                + p2End;
            JsonNode dataNode = generatePeopleDataWithPerson(person);

            String personWithNewAddress = p2Start + addressesStart + p2Address1 + "," + p2Address2 + addressEnd + ","
                + notesWIdDeletedTags + p2End;
            assertThat(
                compoundAccessControlService.hasAccessForAction(generatePeopleDataWithPerson(personWithNewAddress),
                    dataNode, people, USER_ROLES),
                is(false)
            );
        }
    }

    @Nested
    @DisplayName("Compound Field - nested complex fields")
    class CompoundFieldComplexUnderCollectionFieldTests {

        @Test
        @DisplayName("Should grant access when a nested complex child node is deleted and has the required ACLs - whole"
            + " node deleted")
        void shouldGrantAccessWhenNestedComplexChildDeletedAndDeleteACLExists() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + name + "," + birthInfo + personEnd;
            JsonNode existingData = generatePeopleDataWithPerson(p1);

            String p2 = existingPersonStart + name + personEnd;
            JsonNode newData = generatePeopleDataWithPerson(p2); // i.e. with deleted BirthInfo

            assertThat(compoundAccessControlService.hasAccessForAction(newData, existingData, people, USER_ROLES),
                is(true));
        }

        @Test
        @DisplayName("Should grant access when a nested complex child node is deleted and has the required fine "
            + "grained ACLs - whole node deleted")
        void shouldGrantAccessWhenNestedComplexChildDeletedAndFineGrainedDeleteACLExists() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("BirthInfo")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withDelete(true)
                    .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + name + "," + birthInfo + personEnd;
            JsonNode existingData = generatePeopleDataWithPerson(p1);

            String p2 = existingPersonStart + name + personEnd;
            JsonNode newData = generatePeopleDataWithPerson(p2); // i.e. with deleted BirthInfo

            assertThat(compoundAccessControlService.hasAccessForAction(newData, existingData, people, USER_ROLES),
                    is(true));
        }

        @Test
        @DisplayName("Should be OK with empty nested complex child in new data")
        void shouldBeOKWithEmptyNestedComplexFieldInNewData() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + name + "," + birthInfo + personEnd;
            JsonNode existingData = generatePeopleDataWithPerson(p1);

            String p2 = existingPersonStart + name + ",    \"BirthInfo\": {}" + personEnd;
            JsonNode newData = generatePeopleDataWithPerson(p2);

            assertThat(compoundAccessControlService.hasAccessForAction(newData, existingData, people, USER_ROLES),
                is(true));
        }

        @Test
        @DisplayName("Should be OK with empty nested complex child in existing data")
        void shouldBeOKWithEmptyNestedComplexFieldInExistingData() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + name + ",    \"BirthInfo\": {}" + personEnd;
            JsonNode existingData = generatePeopleDataWithPerson(p1);

            String p2 = existingPersonStart + name + "," + birthInfo + personEnd;
            JsonNode newData = generatePeopleDataWithPerson(p2);

            assertThat(compoundAccessControlService.hasAccessForAction(newData, existingData, people, USER_ROLES),
                is(true));
        }

        @Test
        @DisplayName("Should be OK with null nested complex child in new data")
        void shouldBeOKWithNullNestedComplexFieldInNewData() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + name + "," + birthInfo + personEnd;
            JsonNode existingData = generatePeopleDataWithPerson(p1);

            String p2 = existingPersonStart + name + ",    \"BirthInfo\": null" + personEnd;
            JsonNode newData = generatePeopleDataWithPerson(p2);

            assertThat(compoundAccessControlService.hasAccessForAction(newData, existingData, people, USER_ROLES),
                is(true));
        }

        @Test
        @DisplayName("Should be OK with null nested complex child in existing data")
        void shouldBeOKWithNullNestedComplexFieldInExistingData() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));

            final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(people).build();
            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            String p1 = existingPersonStart + name + ",    \"BirthInfo\": null" + personEnd;
            JsonNode existingData = generatePeopleDataWithPerson(p1);

            String p2 = existingPersonStart + name + "," + birthInfo + personEnd;
            JsonNode newData = generatePeopleDataWithPerson(p2);

            assertThat(compoundAccessControlService.hasAccessForAction(newData, existingData, people, USER_ROLES),
                is(true));
        }
    }

    @Nested
    @DisplayName("Compound Field - Collection Under Complex Tests")
    class CompoundFieldCollectionUnderComplexFieldTests {
        private final String noteWOutTags = "{\n"
            + "   \"Txt\": \"someNote11\"\n"
            + "}";
        private final String noteWithANewTag = "{\n"
            + "   \"Txt\": \"someNote11\",\n"
            + "   \"Tags\": [\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"health\",\n"
            + "               \"Category\": \"Personal\"\n"
            + "           },\n"
            + "           \"id\": \"null\"\n"
            + "       }\n"
            + "   ]\n"
            + "}";
        private final String noteWithMultipleNewTags = "{\n"
            + "   \"Txt\": \"someNote11\",\n"
            + "   \"Tags\": [\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"health\",\n"
            + "               \"Category\": \"Personal\"\n"
            + "           },\n"
            + "           \"id\": \"null\"\n"
            + "       },\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"health\",\n"
            + "               \"Category\": \"Personal\"\n"
            + "           },\n"
            + "           \"id\": \"null\"\n"
            + "       },\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"health\",\n"
            + "               \"Category\": \"Personal\"\n"
            + "           },\n"
            + "           \"id\": \"null\"\n"
            + "       },\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"health\",\n"
            + "               \"Category\": \"Personal\"\n"
            + "           },\n"
            + "           \"id\": \"null\"\n"
            + "       }\n"
            + "   ]\n"
            + "}";
        private final String noteWithExisting2Tags = "{\n"
            + "   \"Txt\": \"someNote11\",\n"
            + "   \"Tags\": [\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"business\",\n"
            + "               \"Category\": \"Personal\"\n"
            + "           },\n"
            + "           \"id\": \"3453453\"\n"
            + "       },\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"private\",\n"
            + "               \"Category\": \"Personal\"\n"
            + "           },\n"
            + "           \"id\": \"3453454\"\n"
            + "       }\n"
            + "   ]\n"
            + "}";
        private final String noteWithExisting2TagsUpdated = "{\n"
            + "   \"Txt\": \"someNote11\",\n"
            + "   \"Tags\": [\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"work\",\n"
            + "               \"Category\": \"Personal\"\n"
            + "           },\n"
            + "           \"id\": \"3453453\"\n"
            + "       },\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"private\",\n"
            + "               \"Category\": \"Confidential\"\n"
            + "           },\n"
            + "           \"id\": \"3453454\"\n"
            + "       }\n"
            + "   ]\n"
            + "}";
        private final String noteWithExisting2TagsAndANewTag = "{\n"
            + "   \"Txt\": \"someNote11\",\n"
            + "   \"Tags\": [\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"business\",\n"
            + "               \"Category\": \"Personal\"\n"
            + "           },\n"
            + "           \"id\": \"3453453\"\n"
            + "       },\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"private\",\n"
            + "               \"Category\": \"Personal\"\n"
            + "           },\n"
            + "           \"id\": \"3453454\"\n"
            + "       },\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"health\",\n"
            + "               \"Category\": \"Personal\"\n"
            + "           },\n"
            + "           \"id\": \"null\"\n"
            + "       }\n"
            + "   ]\n"
            + "}";

        private final String noteWithExisting2TagsOneDeleted = "{\n"
            + "   \"Txt\": \"someNote11\",\n"
            + "   \"Tags\": [\n"
            + "       {\n"
            + "           \"value\": {\n"
            + "               \"Tag\": \"private\",\n"
            + "               \"Category\": \"Personal\"\n"
            + "           },\n"
            + "           \"id\": \"3453454\"\n"
            + "       }\n"
            + "   ]\n"
            + "}";
        private final String noteStart = "{\n  \"Note\": \n";
        private final String noteEnd = "  \n}";
        private CaseTypeDefinition caseTypeDefinition;
        private CaseFieldDefinition note;

        @BeforeEach
        void setup() {
            note = newCaseField()
                .withId("Note")
                .withFieldType(aFieldType()
                    .withId("NoteComplex")
                    .withType(COMPLEX)
                    .withComplexField(newCaseField()
                        .withId("Txt")
                        .withFieldType(aFieldType()
                            .withId("Text")
                            .withType("Text")
                            .build())
                        .build())
                    .withComplexField(getTagFieldDefinition())
                    .build())
                .build();
            caseTypeDefinition = newCaseType().withField(note).build();
        }

        @Test
        @DisplayName("Should grant access to add completely new child if child has the required ACLs")
        void shouldGrantAccessToNewChildIfChildrenHasAccess() throws IOException {
            note.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));

            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generateJsonNodeWithData(noteStart + noteWOutTags + noteEnd);

            assertThat(
                compoundAccessControlService.hasAccessForAction(generateJsonNodeWithData(noteStart
                    + noteWithANewTag + noteEnd), dataNode, note, USER_ROLES),
                is(true)
            );
        }

        @Test
        @DisplayName("Should grant access to add multiple completely new children if child has the required ACLs")
        void shouldGrantAccessToMultipleNewChildIfChildrenHasAccess() throws IOException {
            note.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));

            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generateJsonNodeWithData(noteStart + noteWithMultipleNewTags + noteEnd);

            assertThat(
                compoundAccessControlService.hasAccessForAction(generateJsonNodeWithData(noteStart
                    + noteWithANewTag + noteEnd), dataNode, note, USER_ROLES),
                is(true)
            );
        }

        @Test
        @DisplayName("Should grant access to add new child to existing ones if child has the required ACLs")
        void shouldGrantAccessToAddingNewChildIfChildrenHasAccess() throws IOException {
            note.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));

            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generateJsonNodeWithData(noteStart + noteWithExisting2Tags + noteEnd);

            assertThat(
                compoundAccessControlService.hasAccessForAction(
                    generateJsonNodeWithData(noteStart + noteWithExisting2TagsAndANewTag + noteEnd),
                    dataNode, note, USER_ROLES
                ),
                is(true)
            );
        }

        @Test
        @DisplayName("Should deny access to add new child if child has the no ACLs")
        void shouldDenyAccessToNewChildIfChildrenHasAccess() throws IOException {
            note.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(false)
                .build()));

            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generateJsonNodeWithData(noteStart + noteWOutTags + noteEnd);

            assertThat(
                compoundAccessControlService.hasAccessForAction(generateJsonNodeWithData(noteStart
                    + noteWithANewTag + noteEnd), dataNode, note, USER_ROLES),
                is(false)
            );

        }

        @Test
        @DisplayName("Should deny access to add new child to existing ones if child has the required ACLs")
        void shouldDenyAccessToAddingNewChildIfChildrenHasAccess() throws IOException {
            note.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(false)
                .build()));

            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generateJsonNodeWithData(noteStart + noteWithExisting2Tags + noteEnd);

            assertThat(
                compoundAccessControlService.hasAccessForAction(
                    generateJsonNodeWithData(noteStart + noteWithExisting2TagsAndANewTag + noteEnd),
                    dataNode, note, USER_ROLES
                ),
                is(false)
            );
        }

        @Test
        @DisplayName("Should grant access to update child if child has the required ACLs")
        void shouldGrantAccessToUpdateChildIfChildrenHasAccess() throws IOException {
            note.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withUpdate(true)
                .build()));

            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generateJsonNodeWithData(noteStart + noteWithExisting2Tags + noteEnd);

            assertThat(
                compoundAccessControlService.hasAccessForAction(
                    generateJsonNodeWithData(noteStart + noteWithExisting2TagsUpdated + noteEnd),
                    dataNode, note, USER_ROLES
                ),
                is(true)
            );
        }

        @Test
        @DisplayName("Should deny access to update child if child has no ACLs")
        void shouldDenyAccessToUpdateChildIfChildrenHasNoAccess() throws IOException {
            note.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withUpdate(false)
                .build()));

            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generateJsonNodeWithData(noteStart + noteWithExisting2Tags + noteEnd);

            assertThat(
                compoundAccessControlService.hasAccessForAction(
                    generateJsonNodeWithData(noteStart + noteWithExisting2TagsUpdated + noteEnd),
                    dataNode, note, USER_ROLES
                ),
                is(false)
            );
        }

        @Test
        @DisplayName("Should grant access to delete a child if child has the required ACLs")
        void shouldGrantAccessToDeleteChildIfChildrenHasAccess() throws IOException {
            note.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(true)
                .build()));

            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generateJsonNodeWithData(noteStart + noteWithExisting2Tags + noteEnd);

            assertThat(
                compoundAccessControlService.hasAccessForAction(
                    generateJsonNodeWithData(noteStart + noteWithExisting2TagsOneDeleted + noteEnd),
                    dataNode, note, USER_ROLES
                ),
                is(true)
            );
        }

        @Test
        @DisplayName("Should deny access to delete child if child has no ACLs")
        void shouldDenyAccessToDeleteChildIfChildrenHasNoAccess() throws IOException {
            note.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withDelete(false)
                .build()));

            caseTypeDefinition.getCaseFieldDefinitions().stream().forEach(caseField ->
                caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generateJsonNodeWithData(noteStart + noteWithExisting2Tags + noteEnd);

            assertThat(
                compoundAccessControlService.hasAccessForAction(
                    generateJsonNodeWithData(noteStart + noteWithExisting2TagsOneDeleted + noteEnd),
                    dataNode, note, USER_ROLES
                ),
                is(false)
            );
        }
    }

    static JsonNode generatePeopleDataWithPerson(String... args) throws IOException {
        String people = peopleStart;
        for (int i = 0; i < args.length; i++) {
            people += args[i] + (i == args.length - 1 ? "" : ",");
        }
        people = people + peopleEnd;
        final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(people));

        return JacksonUtils.convertValueJsonNode(data);
    }

    static JsonNode generateJsonNodeWithData(String stringData) throws IOException {
        final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(stringData));

        return JacksonUtils.convertValueJsonNode(data);
    }
}
