package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.config.JacksonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.MANDATORY;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.OPTIONAL;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.READONLY;
import static uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinitionTest.findNestedField;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AuditEventBuilder.anAuditEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseStateBuilder.newState;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseUpdateViewEventBuilder.newCaseUpdateViewEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewFieldBuilder.aViewField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.ComplexACLBuilder.aComplexACL;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WizardPageBuilder.newWizardPage;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WizardPageComplexFieldOverrideBuilder.newWizardPageComplexFieldOverride;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

@SuppressWarnings("checkstyle:TypeName") // too many legacy TypeName occurrences on '@Nested' classes
public class AccessControlServiceTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String EVENT_ID_WITH_ACCESS = "EVENT_ID_WITH_ACCESS";
    private static final String EVENT_ID_WITHOUT_ACCESS = "EVENT_ID_WITHOUT_ACCESS";
    private static final String EVENT_ID_WITHOUT_ACCESS_2 = "EVENT_ID_WITHOUT_ACCESS_2";
    private static final String EVENT_ID_WITH_ACCESS_2 = "EVENT_ID_WITH_ACCESS_2";
    static final TypeReference<HashMap<String, JsonNode>> STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static final String ROLE_IN_USER_ROLES = "caseworker-probate-loa1";
    static final String ROLE_IN_USER_ROLES_2 = "caseworker-divorce-loa";
    static final String ROLE_IN_USER_ROLES_3 = "caseworker-probate-loa3";
    static final String ROLE_NOT_IN_USER_ROLES = "caseworker-divorce-loa4";
    static final String ROLE_NOT_IN_USER_ROLES_2 = "caseworker-divorce-loa5";
    private static final String FIRST_CHILD_ID = "46f98326-6c88-426d-82be-d362f0246b7a";
    private static final String SECOND_CHILD_ID = "7c7cfd2a-b5d7-420a-8420-3ac3019cfdc7";
    static final Set<String> USER_ROLES = Sets.newHashSet(ROLE_IN_USER_ROLES,
        ROLE_IN_USER_ROLES_3,
        ROLE_IN_USER_ROLES_2);

    private AccessControlService accessControlService;
    private static final String EVENT_ID = "EVENT_ID";
    private static final String EVENT_ID_LOWER_CASE = "event_id";
    private static final String STATE_ID1 = "State1";
    private static final String STATE_ID2 = "State2";

    static final String person1 = "{\n"
        + "  \"id\": \"1577805e-9584-4994-bfa0-5618846b8918\",\n"
        + "  \"value\": {\n"
        + "    \"FirstName\": \"Fatih\",\n"
        + "    \"LastName\": \"Ozceylan\",\n"
        + "    \"BirthInfo\": {\n"
        + "         \"BornCity\": \"Salihli\",\n"
        + "         \"BornCountry\": \"Turkey\",\n"
        + "         \"BornAddress\": {\n"
        + "               \"Name\": \"work\",\n"
        + "               \"Address\": {"
        + "                   \"Line1\": \"23 Lampton Road\",\n"
        + "                   \"Line2\": \"Fitzgrovia, London\",\n"
        + "                   \"PostCode\": \"EC2 5GN\",\n"
        + "                   \"Country\": \"United Kingdom\"\n"
        + "               }\n"
        + "         }\n"
        + "     },\n"
        + "    \"Addresses\": [\n"
        + "      {\n"
        + "        \"value\": {\n"
        + "           \"Name\": \"home\",\n"
        + "           \"Address\": {"
        + "               \"Line1\": \"106 Dumbledore Close\",\n"
        + "               \"Line2\": \"London\",\n"
        + "               \"PostCode\": \"NP15 6EJ\",\n"
        + "               \"Country\": \"United Kingdom\"\n"
        + "           }\n"
        + "        },\n"
        + "        \"id\": \"13982380030\"\n"
        + "      },\n"
        + "      {\n"
        + "        \"value\": {\n"
        + "          \"Name\": \"work\",\n"
        + "           \"Address\": {"
        + "               \"Line1\": \"41 Kings Road\",\n"
        + "               \"Line2\": \"London\",\n"
        + "               \"PostCode\": \"NP15 6EJ\",\n"
        + "               \"Country\": \"United Kingdom\"\n"
        + "           }\n"
        + "        },\n"
        + "        \"id\": \"123874284787\"\n"
        + "      }\n"
        + "    ],\n"
        + "    \"Notes\": [\n"
        + "      {\n"
        + "        \"value\": {\n"
        + "           \"Txt\": \"someNote11\",\n"
        + "           \"Tags\": [\n"
        + "               {\n"
        + "                   \"value\": {\n"
        + "                       \"Tag\": \"private\",\n"
        + "                       \"Category\": \"Personal\"\n"
        + "                   },\n"
        + "                   \"id\": \"1tak3324dfjk\"\n"
        + "               }\n"
        + "           ]\n"
        + "        },\n"
        + "        \"id\": \"134234\"\n"
        + "      },\n"
        + "      {\n"
        + "        \"value\": {\n"
        + "          \"Txt\": \"someNote21\",\n"
        + "           \"Tags\": [\n"
        + "               {\n"
        + "                   \"value\": {\n"
        + "                       \"Tag\": \"home\",\n"
        + "                       \"Category\": \"Personal\"\n"
        + "                   },\n"
        + "                   \"id\": \"234tak3324dfjk\"\n"
        + "               }\n"
        + "           ]\n"
        + "        },\n"
        + "        \"id\": \"132332e\"\n"
        + "      }\n"
        + "    ]\n"
        + "  }\n"
        + "}";
    static final String p2Start = "    {\n"
        + "      \"id\": \"2577805e-9584-4994-bfa0-5618846b8920\",\n"
        + "      \"value\": {\n";
    static final String p2Names = "        \"FirstName\": \"Andrew\",\n"
        + "        \"LastName\": \"Folga\",\n";
    static final String addressesStart = "        \"Addresses\": [\n";
    static final String p2Address1 = "          {\n"
        + "               \"value\": {\n"
        + "                   \"Name\": \"home\",\n"
        + "                   \"Address\": {"
        + "                       \"Line1\": \"63 Albany Road\",\n"
        + "                       \"Line2\": \"Reading\",\n"
        + "                       \"PostCode\": \"SJ15 6EJ\",\n"
        + "                       \"Country\": \"United Kingdom\"\n"
        + "                   }\n"
        + "               },\n"
        + "               \"id\": \"23982380031\"\n"
        + "          }\n";
    static final String p2Address2 = "          {\n"
        + "               \"value\": {\n"
        + "                   \"Name\": \"work\",\n"
        + "                   \"Address\": {"
        + "                       \"Line1\": \"66 Evgeny Road\",\n"
        + "                       \"Line2\": \"London\",\n"
        + "                       \"PostCode\": \"SW15 4EJ\",\n"
        + "                       \"Country\": \"United Kingdom\"\n"
        + "                   }\n"
        + "               },\n"
        + "               \"id\": \"223874284789\"\n"
        + "          }\n";
    static final String arrayEnd = "        ],\n";
    static final String p2Notes = "        \"Notes\": [\n"
        + "          {\n"
        + "            \"value\": {\n"
        + "               \"Txt\": \"Buy tickets\",\n"
        + "               \"Tags\": [\n"
        + "                   {\n"
        + "                       \"value\": {\n"
        + "                           \"Tag\": \"todos\",\n"
        + "                           \"Category\": \"Work\"\n"
        + "                       },\n"
        + "                       \"id\": \"2tak24dfjk\"\n"
        + "                   }\n"
        + "               ]\n"
        + "            },\n"
        + "            \"id\": \"234212\"\n"
        + "          }\n"
        + "        ]\n";
    static final String p2End = "      }\n"
        + "    }\n";
    static final String person2 = p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + arrayEnd + p2Notes + p2End;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        accessControlService = new AccessControlService(new CompoundAccessControlService());
    }

    @Nested
    @DisplayName("ACL tests - CaseStateDefinition")
    class CanAccessCaseStateWithCriteria_AclTests {

        @Test
        @DisplayName("Should not grant access to case state with relevant acl missing")
        void shouldNotGrantAccessToStateIfRelevantACLMissing() {
            CaseTypeDefinition caseType = newCaseType()
                .withState(newState()
                    .withId(STATE_ID1)
                    .withAcl(anAcl().withRole(ROLE_NOT_IN_USER_ROLES).withCreate(true).withRead(true).build())
                    .build())
                .withState(newState()
                    .withId(STATE_ID2)
                    .withAcl(anAcl().withRole(ROLE_NOT_IN_USER_ROLES).withCreate(true).withRead(true).build())
                    .build())
                .build();

            assertAll(
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID1, caseType, USER_ROLES, CAN_CREATE), is(false)),
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID2, caseType, USER_ROLES, CAN_CREATE), is(false))
            );
        }

        @Test
        @DisplayName("Should not grant access to case state with relevant acl not granting access")
        void shouldNotGrantAccessToStateIfRelevantAclNotGrantingAccess() {
            CaseTypeDefinition caseType = newCaseType()
                .withState(newState()
                    .withId(STATE_ID1)
                    .withAcl(anAcl().withRole(ROLE_IN_USER_ROLES).build())
                    .build())
                .withState(newState()
                    .withId(STATE_ID2)
                    .withAcl(anAcl().withRole(ROLE_IN_USER_ROLES_2).build())
                    .build())
                .build();

            assertAll(
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID1, caseType, USER_ROLES, CAN_CREATE), is(false)),
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID2, caseType, USER_ROLES, CAN_CREATE), is(false))
            );
        }

        @Test
        @DisplayName("Should grant access to case state with acl matching")
        void shouldGrantAccessToStateWithAclMatching() {
            CaseTypeDefinition caseType = newCaseType()
                .withState(newState()
                    .withId(STATE_ID1)
                    .withAcl(anAcl().withRole(ROLE_IN_USER_ROLES).withCreate(true).build())
                    .build())
                .withState(newState()
                    .withId(STATE_ID2)
                    .withAcl(anAcl().withRole(ROLE_IN_USER_ROLES_2).withCreate(true).build())
                    .build())
                .build();

            assertAll(
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID1, caseType, USER_ROLES, CAN_CREATE), is(true)),
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID2, caseType, USER_ROLES, CAN_CREATE), is(true))
            );
        }

        @Test
        @DisplayName("Shouldn't grant access to state when state is not present in definition")
        void shouldNotGrantAccessToStateIfStateIsNotPresentInDefinition() throws IOException {
            CaseTypeDefinition caseType = newCaseType().build();

            assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID1, caseType, USER_ROLES, CAN_CREATE), is(false));
        }

        @Test
        @DisplayName("Should filter states according to acls")
        void shouldFilterStatesAccordingToACLs() {
            CaseStateDefinition caseState1 = newState()
                .withId(STATE_ID1)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build())
                .build();
            CaseStateDefinition caseState2 = newState()
                .withId(STATE_ID2)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .build())
                .build();
            List<CaseStateDefinition> caseStates = new ArrayList<>(asList(caseState1, caseState2));
            final List<CaseStateDefinition> states = accessControlService.filterCaseStatesByAccess(caseStates, USER_ROLES, CAN_READ);

            assertAll(
                () -> assertThat(states.size(), is(1)),
                () -> assertThat(states, hasItem(caseState1)),
                () -> assertThat(states, not(hasItem(caseState2)))
            );
        }

        @Test
        @DisplayName("Should filter states out when no matching ACLs")
        void shouldFilterOutStatesWhenNoMatchingACLSs() {
            CaseStateDefinition caseState1 = newState()
                .withId(STATE_ID1)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .build())
                .build();
            CaseStateDefinition caseState2 = newState()
                .withId(STATE_ID2)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .build())
                .build();
            CaseStateDefinition caseState3 = newState()
                .withId("Some State")
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .build())
                .build();
            List<CaseStateDefinition> caseStates = new ArrayList<>(asList(caseState1, caseState2, caseState3));
            final List<CaseStateDefinition> states = accessControlService.filterCaseStatesByAccess(caseStates, USER_ROLES, CAN_READ);

            assertAll(
                () -> assertThat(states.size(), is(0)),
                () -> assertThat(states, not(hasItem(caseState1))),
                () -> assertThat(states, not(hasItem(caseState2))),
                () -> assertThat(states, not(hasItem(caseState3)))
            );
        }
    }

    @Nested
    @DisplayName("ACL tests")
    class CanAccessCaseFieldsWithCriteria_AclTests {

        @Test
        @DisplayName("Should fail to grant access to fields if acls are missing")
        void shouldFailToGrantCreateAccessForGivenFieldsIfOneFieldIsMissingAcls() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"SomeText\" }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to case fields with relevant acl missing")
        void shouldNotGrantAccessToFieldsIfRelevantAclMissing() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to case fields with relevant acl not granting access")
        void shouldNotGrantAccessToFieldsIfRelevantAclNotGrantingAccess() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Addresses2")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\", "
                    + "   \"Addresses2\": \"someText\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to case fields if ACL false and null value")
        void shouldNotGrantAccessToNullValue() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": null }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should grant access to case fields with acl matching")
        void shouldGrantAccessToFieldsWithAclMatching() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Addresses2")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\", "
                    + "   \"Addresses2\": \"someText\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES,
                    CAN_CREATE),
                is(true));
        }

        @Test
        @DisplayName("Should grant access to case fields when field is no present in definition")
        void shouldGrantAccessToFieldsIfFieldNotPresentInDefinition() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES,
                    CAN_CREATE),
                is(true));
        }
    }

    @Nested
    @DisplayName("text value tests")
    class CanAccessCaseFieldsWithCriteria_TextValueType {

        @Test
        @DisplayName("Should grant access to case fields with text value")
        void shouldGrantAccessToTextValueType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES,
                    CAN_CREATE),
                is(true));
        }

        @Test
        @DisplayName("Should not grant access to case fields if ACL false and empty text")
        void shouldNotGrantAccessToEmptyTextType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }
    }

    @Nested
    @DisplayName("collection value tests")
    class CanAccessCaseFieldsWithCriteria_CollectionValueType {

        @Test
        @DisplayName("Should grant access to case fields with collection")
        void shouldGrantAccessToCollectionType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\":[  \n"
                    + "         {  \n"
                    + "            \"value\":{  \n"
                    + "               \"Address\":\"address1\",\n"
                    + "               \"Notes\": {\n"
                    + "                   \"Note1\": \"someNote11\",\n"
                    + "                   \"Note2\": \"someNote21\"\n"
                    + "                }"
                    + "            },\n"
                    + "            \"id\":\"" + FIRST_CHILD_ID + "\"\n"
                    + "         },\n"
                    + "         {  \n"
                    + "            \"value\":{  \n"
                    + "               \"Address\":\"address1\",\n"
                    + "               \"Notes\": {\n"
                    + "                   \"Note1\": \"someNote21\",\n"
                    + "                   \"Note2\": \"someNote22\"\n"
                    + "                }"
                    + "            },\n"
                    + "            \"id\":\"" + SECOND_CHILD_ID + "\"\n"
                    + "         }\n"
                    + "      ]\n"
                    + "    }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES,
                    CAN_CREATE),
                is(true));
        }

        @Test
        @DisplayName("Should not grant access to case fields if ACL false and empty collection")
        void shouldNotGrantCreateAccessToCollectionTypeIfEmpty() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\":[] }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }
    }

    @Nested
    @DisplayName("complex value tests")
    class CanAccessCaseFieldsWithCriteria_ComplexValueType {

        @Test
        @DisplayName("Should grant access to case fields with complex object")
        void shouldGrantAccessToComplexType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\":{  \n"
                    + "          \"Note\": \"someNote11\"\n"
                    + "       }\n"
                    + "    }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES,
                    CAN_CREATE),
                is(true));
        }

        @Test
        @DisplayName("Should not grant access to case fields if ACL false and empty object")
        void shouldNotGrantAccessToComplexTypeIfEmpty() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\":{} }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }
    }

    @Nested
    @DisplayName("case fields upsert ACL tests")
    class CanAccessCaseFieldsForUpsert_AclTests {

        @Test
        @DisplayName("Should not grant access to field if field acls are missing for update")
        void shouldNotGrantAccessToFieldsIfFieldIsMissingAclsForUpdate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withFieldType(aFieldType().withType("Text").build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\" : \"UpdateAddress\" }");
            JsonNode existingDataNode = getJsonNode("{  \"Addresses\": \"SomeText\" }");

            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to field if field acls are missing for create")
        void shouldNotGrantAccessToFieldsIfFieldIsMissingAclsForCreate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\" : \"CreateAddress\" }\n");
            JsonNode existingDataNode = getJsonNode("{ }");

            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to field if field acls are missing relevant acl for update")
        void shouldNotGrantAccessToFieldsIfFieldIsMissingRelevantAclForUpdate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .withRead(true)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\" : \"UpdateAddress\" }");
            JsonNode existingDataNode = getJsonNode("{  \"Addresses\": \"SomeText\" }");

            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to field if field acls are missing relevant acl for create")
        void shouldNotGrantAccessToFieldsIfFieldIsMissingRelevantAclForCreate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .withRead(true)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\" : \"CreateAddress\" }\n");
            JsonNode existingDataNode = getJsonNode("{ }");

            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to case field with relevant acl not granting access for update")
        void shouldNotGrantAccessToFieldIfRelevantAclNotGrantingAccessForUpdate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\" : \"UpdateAddress\" }\n");
            JsonNode existingDataNode = getJsonNode("{ \"Addresses\": \"SomeText\" }\n");

            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to case field with relevant acl not granting access for create")
        void shouldNotGrantAccessToFieldIfRelevantAclNotGrantingAccessForCreate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\" : \"NewAddress\" }\n");
            JsonNode existingDataNode = getJsonNode("{ }");

            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to case field if ACL false and null value for update")
        void shouldNotGrantAccessToFieldWithNullValueForUpdate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\" : null }\n");
            JsonNode existingDataNode = getJsonNode("{ \"Addresses\": \"SomeText\" }");

            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to case field if ACL false and null value for create")
        void shouldNotGrantAccessToFieldWithNullValueForCreate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\" : null }\n");
            JsonNode existingDataNode = getJsonNode("{ }");

            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to case field if ACL true and field name not matching for update")
        void shouldNotGrantAccessToFieldWithAclAccessGrantedAndFieldNameNotMatchingForUpdate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(true)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"addresses\" : null }\n");
            JsonNode existingDataNode = getJsonNode("{ \"Addresses\": \"SomeText\" }");

            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to case field if ACL true and field name not matching for create")
        void shouldNotGrantAccessToFieldWithAclAccessGrantedAndFieldNameNotMatchingForCreate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(true)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"addresses\" : null }\n");
            JsonNode existingDataNode = getJsonNode("{ }");

            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should grant access to case field with acl matching for update")
        void shouldGrantAccessToFieldWithAclMatchingForUpdate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\": \"UpdateAddress\" }\n");
            JsonNode existingDataNode = getJsonNode("{ \"Addresses\": \"SomeText\" }");

            assertFieldsAccess(true, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should grant access to case field with acl matching for create")
        void shouldGrantAccessToFieldWithAclMatchingForCreate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\": \"CreateAddress\" }\n");
            JsonNode existingDataNode = getJsonNode("{ }");

            assertFieldsAccess(true, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not need to grant access to case field if no value change")
        void shouldNotNeedToGrantAccessToFieldIfNoChangeInValue() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\": \"CreateAddress\" }\n");
            JsonNode existingDataNode = getJsonNode("{ \"Addresses\": \"CreateAddress\" }");

            assertFieldsAccess(true, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should grant access to case fields if all have access granted")
        void shouldGrantAccessToFieldsIfAllFieldsHaveAccessGranted() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("FirstName")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("LastName")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Mobile")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\": \"CreateAddress\","
                + " \"FirstName\": \"John\","
                + " \"LastName\": \"Smith\" }");
            JsonNode existingDataNode = getJsonNode("{ \"FirstName\": \"Mark\","
                + " \"Mobile\": \"07234543543\","
                + " \"LastName\": \"Smith\" }");

            assertFieldsAccess(true, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to case fields if a field does not have access granted")
        void shouldNotGrantAccessToFieldsIfOneFieldDoesNotHaveAccessGranted() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("FirstName")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("LastName")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Mobile")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\": \"CreateAddress\","
                + " \"FirstName\": \"John\","
                + " \"LastName\": \"Smith\" }");
            JsonNode existingDataNode = getJsonNode("{ \"FirstName\": \"Mark\","
                + " \"Mobile\": \"07234543543\","
                + " \"LastName\": \"Smith\" }");


            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to case fields if a field does not have acls")
        void shouldNotGrantAccessToFieldsIfOneFieldDoesNotHaveAcls() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("FirstName")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("LastName")
                    .withFieldType(aFieldType().withType("Text").build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\": \"CreateAddress\","
                + " \"FirstName\": \"John\","
                + " \"LastName\": \"Smith\" }");
            JsonNode existingDataNode = getJsonNode("{ \"FirstName\": \"Mark\","
                + " \"Mobile\": \"07234543543\","
                + " \"LastName\": \"Smith\" }");


            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }
    }

    @Nested
    @DisplayName("case event ACL tests")
    class CanAccessCaseEventWithCriteria_AclTests {

        @Test
        @DisplayName("Should not grant access to event if acls are missing")
        void shouldNotGrantAccessToEventIfEventIsMissingAcls() throws IOException {
            final CaseTypeDefinition caseType = new CaseTypeDefinition();
            CaseEventDefinition eventDefinition = new CaseEventDefinition();
            eventDefinition.setId(EVENT_ID);
            caseType.setEvents(Collections.singletonList(eventDefinition));

            assertThat(
                accessControlService.canAccessCaseEventWithCriteria(
                    EVENT_ID,
                    caseType.getEvents(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to event with relevant acl missing")
        void shouldNotGrantAccessToEventIfRelevantAclMissing() throws IOException {
            final CaseTypeDefinition caseType = new CaseTypeDefinition();
            CaseEventDefinition eventDefinition = new CaseEventDefinition();
            eventDefinition.setId(EVENT_ID);
            AccessControlList accessControlList = new AccessControlList();
            accessControlList.setRole(ROLE_NOT_IN_USER_ROLES);
            accessControlList.setCreate(true);
            List<AccessControlList> accessControlLists = newArrayList(accessControlList);
            eventDefinition.setAccessControlLists(accessControlLists);
            caseType.setEvents(Collections.singletonList(eventDefinition));

            assertThat(
                accessControlService.canAccessCaseEventWithCriteria(
                    EVENT_ID,
                    caseType.getEvents(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to event with relevant acl not granting access")
        void shouldNotGrantAccessToEventIfRelevantAclNotGrantingAccess() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();

            assertThat(
                accessControlService.canAccessCaseEventWithCriteria(
                    EVENT_ID,
                    caseType.getEvents(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to event if ACL false and null value")
        void shouldNotGrantAccessToNullValue() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();

            assertThat(
                accessControlService.canAccessCaseEventWithCriteria(
                    null,
                    caseType.getEvents(),
                    USER_ROLES,
                    AccessControlList::isCreate),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to event if ACL true and event name not matching")
        void shouldNotGrantAccessWithEventNameNotMatching() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            assertThat(
                accessControlService.canAccessCaseEventWithCriteria(
                    EVENT_ID_LOWER_CASE,
                    caseType.getEvents(),
                    USER_ROLES,
                    AccessControlList::isCreate),
                is(false));
        }

        @Test
        @DisplayName("Should grant access to event with acl matching")
        void shouldGrantAccessToEventWithAclMatching() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            assertThat(
                accessControlService.canAccessCaseEventWithCriteria(
                    EVENT_ID,
                    caseType.getEvents(),
                    USER_ROLES,
                    CAN_CREATE),
                is(true));
        }
    }

    @Nested
    @DisplayName("case ACL tests")
    class CanAccessCaseTypeWithCriteria_AclTests {

        @Test
        @DisplayName("Should not grant access to case if acls are missing")
        void shouldNotGrantAccessToCaseIfMissingAcls() throws IOException {
            final CaseTypeDefinition caseType = new CaseTypeDefinition();

            assertThat(
                accessControlService.canAccessCaseTypeWithCriteria(
                    caseType,
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to case with relevant acl missing")
        void shouldNotGrantAccessToCaseIfRelevantAclMissing() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            assertThat(
                accessControlService.canAccessCaseTypeWithCriteria(
                    caseType,
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to case with relevant acl not granting access")
        void shouldNotGrantAccessToCaseIfRelevantAclNotGrantingAccess() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();

            assertThat(
                accessControlService.canAccessCaseTypeWithCriteria(
                    caseType,
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should grant access to case with acl matching")
        void shouldGrantAccessToCaseWithAclMatching() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES_3)
                    .build())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build())
                .build();

            assertThat(
                accessControlService.canAccessCaseTypeWithCriteria(
                    caseType,
                    USER_ROLES,
                    CAN_CREATE),
                is(true));
        }
    }

    @Nested
    @DisplayName("case fields ACL tests")
    class ReturnsDataWithCaseFieldReadAccess_AclTests {

        @Test
        @DisplayName("Should not return data if field acls are missing")
        void shouldNotReturnDataIfCaseFieldIsMissingAcls() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"SomeText\" }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should not return data if field with relevant acl missing")
        void shouldNotReturnFieldIfRelevantAclMissing() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should not return data if field with relevant acl not granting access")
        void shouldNotGrantAccessToFieldsIfRelevantAclNotGrantingAccess() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should not return data if field with acl false and null value")
        void shouldNotReturnDataWithAclFalseAndNullValue() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": null }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should not return data if field with acl true and field name not matching")
        void shouldNotReturnDataWithAclTrueAndFieldNameNotMatching() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"addresses\": \"someText\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should return data if field with acl true and null value")
        void shouldReturnDataWithAclTrueAndNullValue() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": null }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(dataNode)));
        }

        @Test
        @DisplayName("Should return data if field with acl true and empty value")
        void shouldReturnDataWithAclTrueAndEmptyValue() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(dataNode)));
        }

        @Test
        @DisplayName("Should return data if field with acl matching")
        void shouldGrantAccessToFieldsWithAclMatching() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(dataNode)));
        }
    }

    @Nested
    @DisplayName("return fields data with text value tests")
    class ReturnsDataWithCaseFieldAccess_TextValueType {

        @Test
        @DisplayName("Should not return data if field ACL false and empty text")
        void shouldNotGrantAccessToEmptyTextType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should return data with null and empty values on root level")
        void shouldReturnDataWithNullAndEmptyValuesOnRootLevel() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Addresses2")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": null,\n"
                    + "       \"Addresses2\": \"\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertAll(
                () -> assertThat(jsonNode.get("Addresses"), is(JSON_NODE_FACTORY.nullNode())),
                () -> assertThat(jsonNode.get("Addresses2"), is(getTextNode("")))
            );
        }
    }

    @Nested
    @DisplayName("return fields data with collection value tests")
    class ReturnsDataWithCaseFieldAccess_CollectionValueType {
        private static final String VALUE = "value";
        private static final String ID = "id";

        @Test
        @DisplayName("Should return data if field with collection")
        void shouldGrantAccessToCollectionType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{\n"
                    + "         \"Addresses\":[  \n"
                    + "         {  \n"
                    + "            \"value\":{  \n"
                    + "               \"Address\":\"address1\",\n"
                    + "               \"Notes\": {\n"
                    + "                   \"Note1\": \"someNote11\",\n"
                    + "                   \"Note2\": \"someNote21\"\n"
                    + "                }"
                    + "            },\n"
                    + "            \"id\":\"" + FIRST_CHILD_ID + "\"\n"
                    + "         },\n"
                    + "         {  \n"
                    + "            \"value\":{  \n"
                    + "               \"Address\":\"address2\",\n"
                    + "               \"Notes\": {\n"
                    + "                   \"Note1\": \"someNote21\",\n"
                    + "                   \"Note2\": \"someNote22\"\n"
                    + "                }"
                    + "            },\n"
                    + "            \"id\":\"" + SECOND_CHILD_ID + "\"\n"
                    + "         }\n"
                    + "      ]\n"
                    + "    }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(dataNode)));
        }

        @Test
        @DisplayName("Should return data if field and children have ACLs")
        void shouldGrantAccessToCollectionTypeChildren() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withRead(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("FirstName")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("LastName")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build()
            ));

            final CaseTypeDefinition caseType = newCaseType().withField(people).build();
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleData();

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertAll(
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("FirstName").textValue(), is("Fatih")),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("LastName"), is(nullValue())),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("Addresses").get(0).get("value").get("Name").textValue(), is("home")),
                () -> assertThat(
                    jsonNode.get("People").get(0).get("value").get("Addresses").get(1).get("value").get("Address").get("Line1").textValue(),
                    is("41 Kings Road")
                ),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("Notes").get(0).get("value").get("Txt").textValue(), is("someNote11")),
                () -> assertThat(jsonNode.get("People").get(0).get("value").size(), is(3))
            );
        }

        @Test
        @DisplayName("Should filter data when child doesnot have ACLs")
        void shouldfilterDataWhenChildDoesnotHaveACL() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withRead(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("FirstName")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("LastName")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornCity")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornAddress")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornAddress.Address")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Name")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes.Txt")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build()
            ));

            final CaseTypeDefinition caseType = newCaseType().withField(people).build();
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            JsonNode dataNode = generatePeopleData();

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertAll(
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("FirstName").textValue(), is("Fatih")),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("LastName"), is(nullValue())),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("BirthInfo").get("BornCity").textValue(), is("Salihli")),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("BirthInfo").get("BornCountry"), is(nullValue())),
                () -> assertThat(
                    jsonNode.get("People").get(0).get("value").get("BirthInfo").get("BornAddress").get("Address").get("Line1").textValue(),
                    is("23 Lampton Road")
                ),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("Addresses").get(0).get("value").get("Name").textValue(), is("home")),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("Addresses").get(0).get("value").get("Address"), is(nullValue())),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("Notes").get(0).get("value").get("Note"), is(nullValue())),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("Notes").get(0).get("value").get("Txt").textValue(), is("someNote11")),
                () -> assertThat(jsonNode.get("People").get(0).get("value").size(), is(4))
            );
        }

        @Test
        @DisplayName("Should return data with null and empty values on root level")
        void shouldReturnDataWithNullAndEmptyValuesOnRootLevel() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Addresses2")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Addresses3")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{    \n"
                    + "         \"Addresses\":[  \n"
                    + "         {  \n"
                    + "            \"value\":{  \n"
                    + "               \"Address\":\"address1\",\n"
                    + "               \"Notes\": {\n"
                    + "                   \"Note1\": \"someNote11\",\n"
                    + "                   \"Note2\": \"someNote21\"\n"
                    + "                }"
                    + "            },\n"
                    + "            \"id\":\"" + FIRST_CHILD_ID + "\"\n"
                    + "         },\n"
                    + "         {  \n"
                    + "            \"value\":{  \n"
                    + "               \"Address\":\"address2\",\n"
                    + "               \"Notes\": {\n"
                    + "                   \"Note1\": \"someNote21\",\n"
                    + "                   \"Note2\": \"someNote22\"\n"
                    + "                }"
                    + "            },\n"
                    + "            \"id\":\"" + SECOND_CHILD_ID + "\"\n"
                    + "         }\n"
                    + "        ],\n"
                    + "      \"Addresses2\": [],\n"
                    + "      \"Addresses3\": null\n"
                    + "    }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertAll(
                () -> assertThat(jsonNode.has("Addresses2"), is(true)),
                () -> assertThat(jsonNode.get("Addresses2"), is(JSON_NODE_FACTORY.arrayNode())),
                () -> assertThat(jsonNode.has("Addresses3"), is(true)),
                () -> assertThat(jsonNode.get("Addresses3"), is(JSON_NODE_FACTORY.nullNode())),
                () -> assertThat(jsonNode.get("Addresses").get(0).get(ID), is(getTextNode(FIRST_CHILD_ID))),
                () -> assertThat(jsonNode.get("Addresses").get(0).get(VALUE).get("Address"),
                    is(getTextNode("address1"))),
                () -> assertThat(jsonNode.get("Addresses").get(0).get(VALUE).get("Notes").get("Note1"),
                    is(getTextNode("someNote11"))),
                () -> assertThat(jsonNode.get("Addresses").get(0).get(VALUE).get("Notes").get("Note2"),
                    is(getTextNode("someNote21"))),
                () -> assertThat(jsonNode.get("Addresses").get(1).get(ID), is(getTextNode(SECOND_CHILD_ID))),
                () -> assertThat(jsonNode.get("Addresses").get(1).get(VALUE).get("Address"),
                    is(getTextNode("address2"))),
                () -> assertThat(jsonNode.get("Addresses").get(1).get(VALUE).get("Notes").get("Note1"),
                    is(getTextNode("someNote21"))),
                () -> assertThat(jsonNode.get("Addresses").get(1).get(VALUE).get("Notes").get("Note2"),
                    is(getTextNode("someNote22")))
            );
        }


        @Test
        @DisplayName("Should not return data if field ACL false and empty collection")
        void shouldNotGrantAccessToCollectionTypeIfEmpty() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\":[] }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }
    }

    @Nested
    @DisplayName("return fields data with complex value tests")
    class ReturnsDataWithCaseFieldReadAccess_ComplexValueType {

        @Test
        @DisplayName("Should return data if field with complex object")
        void shouldGrantAccessToComplexType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\":{  \n"
                    + "           \"Note\": \"someNote11\"\n"
                    + "       }\n"
                    + "    }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode.get("Addresses").get("Note"), is(getTextNode("someNote11")));
        }

        @Test
        @DisplayName("Should return data with null and empty values on root level")
        void shouldReturnDataWithNullAndEmptyValuesOnRootLevel() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Addresses2")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": null,\n"
                    + "      \"Addresses2\":{  \n"
                    + "           \"Note\": \"\"\n"
                    + "       }\n"
                    + "    }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertAll(
                () -> assertThat(jsonNode.get("Addresses"), is(JSON_NODE_FACTORY.nullNode())),
                () -> assertThat(jsonNode.get("Addresses2").get("Note"), is(getTextNode("")))
            );
        }

        @Test
        @DisplayName("Should not return data if field ACL false and empty object")
        void shouldNotGrantAccessToComplexTypeIfEmpty() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            List<CaseFieldDefinition> caseFields = newArrayList();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\":{} }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }
    }

    @Nested
    @DisplayName("case type ACL tests")
    class ReturnsDataWithCaseType_AclTests {

        @Test
        @DisplayName("Should not return event if event is missing")
        void shouldNotReturnEventIfCaseEventIsMissing() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .build())
                .build();
            List<AuditEvent> auditEvents = null;

            assertThat(accessControlService.filterCaseAuditEventsByReadAccess(auditEvents,
                caseType.getEvents(),
                USER_ROLES),
                is(emptyCollectionOf(AuditEvent.class)));
        }
    }

    @Nested
    @DisplayName("case audit events ACL tests")
    class ReturnsDataWithCaseEventReadAccess_AclTests {

        @Test
        @DisplayName("Should not return audit event if event is missing")
        void shouldNotReturnEventIfCaseEventIsMissing() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .build())
                .build();
            List<AuditEvent> auditEvents = null;

            assertThat(accessControlService.filterCaseAuditEventsByReadAccess(auditEvents,
                caseType.getEvents(),
                USER_ROLES),
                is(emptyCollectionOf(AuditEvent.class)));
        }

        @Test
        @DisplayName("Should not return audit event if acls are missing")
        void shouldNotReturnEventIfCaseEventIsMissingAcls() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .build())
                .build();
            List<AuditEvent> auditEvents = newArrayList(anAuditEvent()
                .withEventId(EVENT_ID)
                .build());

            assertThat(accessControlService.filterCaseAuditEventsByReadAccess(auditEvents,
                caseType.getEvents(),
                USER_ROLES),
                is(emptyCollectionOf(AuditEvent.class)));
        }

        @Test
        @DisplayName("Should not return audit event if relevant acl missing")
        void shouldNotReturnEventIfRelevantAclMissing() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            List<AuditEvent> auditEvents = newArrayList(anAuditEvent()
                .withEventId(EVENT_ID)
                .build());

            assertThat(accessControlService.filterCaseAuditEventsByReadAccess(auditEvents,
                caseType.getEvents(),
                USER_ROLES),
                is(emptyCollectionOf(AuditEvent.class)));
        }

        @Test
        @DisplayName("Should not return audit event if relevant acl not granting access")
        void shouldNotReturnEventIfRelevantAclNotGrantingAccess() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            List<AuditEvent> auditEvents = newArrayList(anAuditEvent()
                .withEventId(EVENT_ID)
                .build());

            assertThat(accessControlService.filterCaseAuditEventsByReadAccess(auditEvents,
                caseType.getEvents(),
                USER_ROLES),
                is(emptyCollectionOf(AuditEvent.class)));
        }

        @Test
        @DisplayName("Should not return audit event if ACL true and event name not matching")
        void shouldNotReturnEventIfRelevantAclGrantingAccessAndEventNameNotMatching() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();
            List<AuditEvent> auditEvents = newArrayList(anAuditEvent()
                .withEventId(EVENT_ID_LOWER_CASE)
                .build());

            assertThat(accessControlService.filterCaseAuditEventsByReadAccess(auditEvents,
                caseType.getEvents(),
                USER_ROLES),
                is(emptyCollectionOf(AuditEvent.class)));
        }

        @Test
        @DisplayName("Should return audit event if acl matching")
        void shouldReturnEventWithAclMatching() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            List<AuditEvent> auditEvents = newArrayList(anAuditEvent()
                .withEventId(EVENT_ID)
                .build());

            assertThat(accessControlService.filterCaseAuditEventsByReadAccess(
                auditEvents,
                caseType.getEvents(),
                USER_ROLES),
                is(auditEvents));
        }

        @Test
        @DisplayName("Should return single audit event if acl matching from a group")
        void shouldReturnEventWithAclMatchingFromGroup() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES_2)
                        .withRead(true)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            List<AuditEvent> auditEvents = newArrayList(anAuditEvent()
                .withEventId(EVENT_ID)
                .build());

            assertThat(accessControlService.filterCaseAuditEventsByReadAccess(auditEvents,
                caseType.getEvents(),
                USER_ROLES),
                is(auditEvents));
        }

        @Test
        @DisplayName("Should return audit events if acls matching")
        void shouldReturnEventsWithAclsMatching() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent().withId(EVENT_ID_WITH_ACCESS)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .build())
                    .build())
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID_WITHOUT_ACCESS)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .build())
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID_WITHOUT_ACCESS_2)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build())
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID_WITH_ACCESS_2)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withRead(true)
                        .build())
                    .build())
                .build();

            List<AuditEvent> auditEvents = newArrayList(anAuditEvent()
                    .withEventId(EVENT_ID_WITH_ACCESS)
                    .build(),
                anAuditEvent()
                    .withEventId(EVENT_ID_WITHOUT_ACCESS)
                    .build(),
                anAuditEvent()
                    .withEventId(EVENT_ID_WITHOUT_ACCESS_2)
                    .build(),
                anAuditEvent()
                    .withEventId(EVENT_ID_WITH_ACCESS_2)
                    .build());

            List<AuditEvent> actual = accessControlService.filterCaseAuditEventsByReadAccess(
                auditEvents,
                caseType.getEvents(),
                USER_ROLES);
            assertThat(actual, containsInAnyOrder(hasProperty("eventId", is("EVENT_ID_WITH_ACCESS")),
                hasProperty("eventId", is("EVENT_ID_WITH_ACCESS_2"))));
        }
    }


    @Nested
    @DisplayName("case event trigger ACL tests")
    class ReturnsCaseEventTriggerDataWithCaseFieldReadonly_AclTests {

        @Test
        @DisplayName("Should set readonly flag if relevant acl missing")
        void shouldSetReadonlyFlagIfRelevantAclMissing() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType("Text").build())
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(true)
                        .withRead(true)
                        .build())
                    .build())
                .build();

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(
                    aViewField()
                        .withFieldType(aFieldType().withType("Text").build())
                        .withId("Addresses")
                        .build())
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext", is(READONLY))));
        }

        @Test
        @DisplayName("Should set readonly flag for complex children if relevant acl missing")
        void shouldSetReadonlyFlagForComplexChildrenIfRelevantAclMissing() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType()
                        .withType(COMPLEX)
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType("Text")
                                        .withId("Text")
                                        .build())
                                .withId("Line1")
                                .build())
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType("Text")
                                        .withId("Text")
                                        .build())
                                .withId("Line2")
                                .build())
                        .build())
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(false)
                        .withRead(true)
                        .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode("Line1")
                            .withRole(ROLE_IN_USER_ROLES)
                            .withCreate(false)
                            .withUpdate(false)
                            .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode("Line2")
                            .withRole(ROLE_IN_USER_ROLES)
                            .withCreate(true)
                            .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(
                    aViewField()
                        .withFieldType(aFieldType()
                            .withType(COMPLEX)
                            .withComplexField(
                                newCaseField()
                                    .withFieldType(
                                        aFieldType()
                                            .withType("Text")
                                            .withId("Text")
                                            .build())
                                    .withId("Line1")
                                    .build())
                            .withComplexField(
                                newCaseField()
                                    .withFieldType(
                                        aFieldType()
                                            .withType("Text")
                                            .withId("Text")
                                            .build())
                                    .withId("Line2")
                                    .build())
                            .build())
                        .withId("Addresses")
                        .build())
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext", is(READONLY)))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), "Line1"), hasProperty("displayContext", is(READONLY))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), "Line2"), hasProperty("displayContext", is(READONLY)))
            );
        }

        @Test
        @DisplayName("Should not set readonly flag for complex children if relevant acl is there")
        void shouldNotSetReadonlyFlagForComplexChildrenIfRelevantAclIsThere() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType()
                        .withType(COMPLEX)
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType("Text")
                                        .withId("Text")
                                        .build())
                                .withId("Line1")
                                .build())
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType("Text")
                                        .withId("Text")
                                        .build())
                                .withId("Line2")
                                .build())
                        .build())
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(true)
                        .withRead(true)
                        .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode("Line1")
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(true)
                            .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode("Line2")
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(true)
                            .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(
                    aViewField()
                        .withFieldType(aFieldType()
                            .withType(COMPLEX)
                            .withComplexField(
                                newCaseField()
                                    .withFieldType(
                                        aFieldType()
                                            .withType("Text")
                                            .withId("Text")
                                            .build())
                                    .withId("Line1")
                                    .build())
                            .withComplexField(
                                newCaseField()
                                    .withFieldType(
                                        aFieldType()
                                            .withType("Text")
                                            .withId("Text")
                                            .build())
                                    .withId("Line2")
                                    .build())
                            .build())
                        .withId("Addresses")
                        .build())
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields().get(0).getDisplayContext(), not(READONLY)),
                () -> assertThat(eventTrigger.getCaseFields().get(0).getComplexFieldNestedField("Line1"), not(hasProperty("displayContext", is(READONLY)))),
                () -> assertThat(eventTrigger.getCaseFields().get(0).getComplexFieldNestedField("Line2"), not(hasProperty("displayContext", is(READONLY))))
            );
        }

        @Test
        @DisplayName("Should set readonly flag for complex children and complex field overrides if relevant acl is missing")
        void shouldSetReadonlyFlagForComplexChildrenIfRelevantAclIsMissing() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType()
                        .withType(COMPLEX)
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType("Text")
                                        .withId("Text")
                                        .build())
                                .withId("Line1")
                                .build())
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType("Text")
                                        .withId("Text")
                                        .build())
                                .withId("Line2")
                                .build())
                        .build())
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(true)
                        .withRead(true)
                        .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode("Line1")
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(true)
                            .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode("Line2")
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(false)
                            .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            final CaseViewField caseViewField1 = aViewField()
                .withFieldType(aFieldType()
                    .withType(COMPLEX)
                    .withComplexField(
                        newCaseField()
                            .withFieldType(
                                aFieldType()
                                    .withType("Text")
                                    .withId("Text")
                                    .build())
                            .withId("Line1")
                            .build())
                    .withComplexField(
                        newCaseField()
                            .withFieldType(
                                aFieldType()
                                    .withType("Text")
                                    .withId("Text")
                                    .build())
                            .withId("Line2")
                            .build())
                    .build())
                .withId("Addresses")
                .build();
            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1, asList(
                        newWizardPageComplexFieldOverride()
                            .withComplexFieldId("Addresses.Line1")
                            .withDisplayContext(OPTIONAL)
                            .build(),
                        newWizardPageComplexFieldOverride()
                            .withComplexFieldId("Addresses.Line2")
                            .withDisplayContext(MANDATORY)
                            .build()))
                    .build()
                )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields().get(0).getDisplayContext(), not(READONLY)),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField("Line1").orElseThrow(() -> new RuntimeException("Line 2 is not there")),
                    not(hasProperty("displayContext", is(READONLY)))
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField("Line2").orElseThrow(() -> new RuntimeException("Line 2 is not there")),
                    hasProperty("displayContext", is(READONLY))
                ),
                () -> assertThat(
                    eventTrigger.getWizardPages().get(0).getWizardPageFields().get(0).getComplexFieldOverrides().get(0).getDisplayContext(),
                    is(OPTIONAL)
                ),
                () -> assertThat(
                    eventTrigger.getWizardPages().get(0).getWizardPageFields().get(0).getComplexFieldOverrides().get(1).getDisplayContext(),
                    is(READONLY)
                )
            );
        }

        @Test
        @DisplayName("Should set readonly flag for collection children if relevant acl missing")
        void shouldSetReadonlyFlagForCollectionChildrenIfRelevantAclMissing() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("AddressCollection")
                    .withFieldType(aFieldType()
                        .withType(COLLECTION)
                        .withCollectionField(newCaseField()
                            .withId("Addresses")
                            .withFieldType(aFieldType()
                                .withType(COMPLEX)
                                .withComplexField(newCaseField()
                                    .withId("Line1")
                                    .withFieldType(aFieldType()
                                        .withId("Text")
                                        .withType("Text")
                                        .build())
                                    .build())
                                .withComplexField(newCaseField()
                                    .withId("Line2")
                                    .withFieldType(aFieldType()
                                        .withId("Text")
                                        .withType("Text")
                                        .build())
                                    .build())
                                .build())
                            .build())
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode("Addresses")
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(true)
                            .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode("Addresses.Line1")
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(false)
                            .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode("Addresses.Line2")
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(false)
                            .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(aViewField()
                    .withId("AddressCollection")
                    .withFieldType(aFieldType()
                        .withType(COLLECTION)
                        .withCollectionField(newCaseField()
                            .withId("Addresses")
                            .withFieldType(aFieldType()
                                .withType(COMPLEX)
                                .withComplexField(newCaseField()
                                    .withId("Line1")
                                    .withFieldType(aFieldType()
                                        .withId("Text")
                                        .withType("Text")
                                        .build())
                                    .build())
                                .withComplexField(
                                    newCaseField()
                                        .withId("Line2")
                                        .withFieldType(aFieldType()
                                            .withId("Text")
                                            .withType("Text")
                                            .build())
                                        .build())
                                .build())
                            .build())
                        .build())
                    .build())
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields().get(0), not(hasProperty("displayContext", is(READONLY)))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), "Addresses"), not(hasProperty("displayContext", is(READONLY)))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), "Addresses.Line1"), hasProperty("displayContext", is(READONLY))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), "Addresses.Line2"), hasProperty("displayContext", is(READONLY)))
            );
        }

        @Test
        @DisplayName("Should not set readonly flag for collection children if relevant acl is there")
        void shouldNotSetReadonlyFlagForCollectionChildrenIfRelevantAclIsThere() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("AddressCollection")
                    .withFieldType(aFieldType()
                        .withType(COLLECTION)
                        .withCollectionField(newCaseField()
                            .withId("Addresses")
                            .withFieldType(aFieldType()
                                .withType(COMPLEX)
                                .withComplexField(newCaseField()
                                    .withId("Line1")
                                    .withFieldType(aFieldType()
                                        .withId("Text")
                                        .withType("Text")
                                        .build())
                                    .build())
                                .withComplexField(newCaseField()
                                    .withId("Line2")
                                    .withFieldType(aFieldType()
                                        .withId("Text")
                                        .withType("Text")
                                        .build())
                                    .build())
                                .build())
                            .build())
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode("Addresses")
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(true)
                            .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode("Addresses.Line1")
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(true)
                            .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode("Addresses.Line2")
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(true)
                            .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(aViewField()
                    .withId("AddressCollection")
                    .withFieldType(aFieldType()
                        .withType(COLLECTION)
                        .withCollectionField(newCaseField()
                            .withId("Addresses")
                            .withFieldType(aFieldType()
                                .withType(COMPLEX)
                                .withComplexField(newCaseField()
                                    .withId("Line1")
                                    .withFieldType(aFieldType()
                                        .withId("Text")
                                        .withType("Text")
                                        .build())
                                    .build())
                                .withComplexField(
                                    newCaseField()
                                        .withId("Line2")
                                        .withFieldType(aFieldType()
                                            .withId("Text")
                                            .withType("Text")
                                            .build())
                                        .build())
                                .build())
                            .build())
                        .build())
                    .build())
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields().get(0), not(hasProperty("displayContext", is(READONLY)))),
                () -> assertThat(eventTrigger.getCaseFields().get(0).getComplexFieldNestedField("Addresses"), not(hasProperty("displayContext", is(READONLY)))),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField("Addresses.Line1"), not(hasProperty("displayContext",
                    is(READONLY)))
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField("Addresses.Line2"), not(hasProperty("displayContext",
                    is(READONLY)))
                )
            );
        }

        @Test
        @DisplayName("Should set readonly flag if relevant acl not granting access")
        void shouldSetReadonlyFlagIfRelevantAclNotGrantingAccess() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType("Text").build())
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(
                    aViewField()
                        .withFieldType(aFieldType().withType("Text").build())
                        .withId("Addresses")
                        .build())
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext", is(READONLY))));
        }

        @Test
        @DisplayName("Should set readonly flag if ACL true and event name not matching")
        void shouldSetReadonlyFlagIfRelevantAclGrantingAccessAndEventNameNotMatching() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();
            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(
                    aViewField()
                        .withId("DifferentAddresses")
                        .build())
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext", is(READONLY))));
        }

        @Test
        @DisplayName("Should not set readonly flag if acl matching")
        void shouldNotSetReadonlyFlagIfAclMatching() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType("Text").build())
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();
            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(
                    aViewField()
                        .withFieldType(aFieldType().withType("Text").build())
                        .withId("Addresses")
                        .build())
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(not(hasProperty("displayContext", is(READONLY)))));
        }

        @Test
        @DisplayName("Should not set readonly flag if acl matching in acls group")
        void shouldNotSetReadonlyFlagIfAclMatchingInAclsGroup() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType("Text").build())
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES_2)
                        .withUpdate(true)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();
            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(
                    aViewField()
                        .withFieldType(aFieldType().withType("Text").build())
                        .withId("Addresses")
                        .build())
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(not(hasProperty("displayContext", is(READONLY)))));

        }

        @Test
        @DisplayName("Should not set readonly flags if acls matching in fields group")
        void shouldNotSetReadonlyFlagsIfAclsMatchingInCaseViewFieldsGroup() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType("Text").build())
                    .withId("Addresses")
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType("Text").build())
                    .withId("AddressesNoAccess")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType("Text").build())
                    .withId("AddressesNoAccess2")
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType("Text").build())
                    .withId("Addresses2")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(
                    aViewField()
                        .withFieldType(aFieldType().withType("Text").build())
                        .withId("Addresses")
                        .build())
                .withField(
                    aViewField()
                        .withFieldType(aFieldType().withType("Text").build())
                        .withId("AddressesNoAccess")
                        .build())
                .withField(
                    aViewField()
                        .withFieldType(aFieldType().withType("Text").build())
                        .withId("AddressesNoAccess2")
                        .build())
                .withField(
                    aViewField()
                        .withFieldType(aFieldType().withType("Text").build())
                        .withId("Addresses2")
                        .build())
                .build();

            CaseUpdateViewEvent actual = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);
            assertAll(
                () -> assertThat(actual.getCaseFields(), hasSize(4)),
                () -> assertThat(actual.getCaseFields(), hasItem(allOf(hasProperty("id", is("Addresses")),
                    not(hasProperty("displayContext", is(READONLY)))))),
                () -> assertThat(actual.getCaseFields(), hasItem(allOf(hasProperty("id", is("AddressesNoAccess")),
                    hasProperty("displayContext", is(READONLY))))),
                () -> assertThat(actual.getCaseFields(), hasItem(allOf(hasProperty("id", is("AddressesNoAccess2")),
                    hasProperty("displayContext", is(READONLY))))),
                () -> assertThat(actual.getCaseFields(), hasItem(allOf(hasProperty("id", is("Addresses2")),
                    not(hasProperty("displayContext", is(READONLY))))))
            );
        }
    }

    @Nested
    @DisplayName("case event definitions ACL tests")
    class ReturnsCaseEventsDataWithCaseEventAccess_AclTests {

        @Test
        @DisplayName("Should not return case event definition if relevant acl missing")
        void shouldNotReturnCaseEventDefinitionIfRelevantAclMissing() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .withRead(true)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();

            assertThat(accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
                USER_ROLES,
                CAN_CREATE),
                is(emptyCollectionOf(CaseEventDefinition.class)));
        }

        @Test
        @DisplayName("Should not return case event definition if relevant acl not granting access")
        void shouldNotReturnCaseEventDefinitionIfRelevantAclNotGrantingAccess() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(false)
                        .withRead(true)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();

            assertThat(accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
                USER_ROLES,
                CAN_CREATE),
                is(emptyCollectionOf(CaseEventDefinition.class)));
        }

        @Test
        @DisplayName("Should return case event definition if acl matching")
        void shouldReturnCaseEventDefinitionWithAclMatching() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            List<CaseEventDefinition> result = accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
                USER_ROLES,
                CAN_CREATE);

            assertAll(
                () -> assertThat(result, hasSize(1)),
                () -> assertThat(result, hasItem(hasProperty("id", is(EVENT_ID))))
            );
        }

        @Test
        @DisplayName("Should return single case event definition if acl matching from a group")
        void shouldReturnCaseEventDefinitionWithAclMatchingFromGroup() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            List<CaseEventDefinition> result = accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
                USER_ROLES,
                CAN_CREATE);
            assertAll(
                () -> assertThat(result, hasSize(1)),
                () -> assertThat(result, hasItem(hasProperty("id", is(EVENT_ID))))
            );
        }

        @Test
        @DisplayName("Should return case event definition if acls matching")
        void shouldReturnCaseEventDefinitionWithAclsMatching() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID_WITH_ACCESS)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .build())
                    .build())
                .withEvent(newCaseEvent().withId(EVENT_ID_WITHOUT_ACCESS)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .build())
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID_WITHOUT_ACCESS_2)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID_WITH_ACCESS_2)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            List<CaseEventDefinition> result = accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
                USER_ROLES,
                CAN_CREATE);
            assertAll(
                () -> assertThat(result, hasSize(2)),
                () -> assertThat(result, hasItem(hasProperty("id", is("EVENT_ID_WITH_ACCESS")))),
                () -> assertThat(result, hasItem(hasProperty("id", is("EVENT_ID_WITH_ACCESS_2"))))
            );
        }
    }

    @Nested
    @DisplayName("case view fields ACL tests")
    class CaseViewFields_AclTests {

        @Test
        @DisplayName("Should not return case event definition if relevant acl missing")
        void shouldNotReturnCaseEventDefinitionIfRelevantAclMissing() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .withRead(true)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();

            assertThat(accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
                USER_ROLES,
                CAN_CREATE),
                is(emptyCollectionOf(CaseEventDefinition.class)));
        }

        @Test
        @DisplayName("Should not return case event definition if relevant acl not granting access")
        void shouldNotReturnCaseEventDefinitionIfRelevantAclNotGrantingAccess() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(false)
                        .withRead(true)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();

            assertThat(accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
                USER_ROLES,
                CAN_CREATE),
                is(emptyCollectionOf(CaseEventDefinition.class)));
        }

        @Test
        @DisplayName("Should return case event definition if acl matching")
        void shouldReturnCaseEventDefinitionWithAclMatching() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            List<CaseEventDefinition> result = accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
                USER_ROLES,
                CAN_CREATE);

            assertAll(
                () -> assertThat(result, hasSize(1)),
                () -> assertThat(result, hasItem(hasProperty("id", is(EVENT_ID))))
            );
        }

        @Test
        @DisplayName("Should return single case event definition if acl matching from a group")
        void shouldReturnCaseEventDefinitionWithAclMatchingFromGroup() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            List<CaseEventDefinition> result = accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
                USER_ROLES,
                CAN_CREATE);
            assertAll(
                () -> assertThat(result, hasSize(1)),
                () -> assertThat(result, hasItem(hasProperty("id", is(EVENT_ID))))
            );
        }

        @Test
        @DisplayName("Should return case event definition if acls matching")
        void shouldReturnCaseEventDefinitionWithAclsMatching() throws IOException {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID_WITH_ACCESS)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .build())
                    .build())
                .withEvent(newCaseEvent().withId(EVENT_ID_WITHOUT_ACCESS)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .build())
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID_WITHOUT_ACCESS_2)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID_WITH_ACCESS_2)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            List<CaseEventDefinition> result = accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
                USER_ROLES,
                CAN_CREATE);
            assertAll(
                () -> assertThat(result, hasSize(2)),
                () -> assertThat(result, hasItem(hasProperty("id", is("EVENT_ID_WITH_ACCESS")))),
                () -> assertThat(result, hasItem(hasProperty("id", is("EVENT_ID_WITH_ACCESS_2"))))
            );
        }
    }

    @Nested
    @DisplayName("CRUD contract on collection")
    class CRUDonCollection {
        private JsonNode existingDataNode;
        private String comma = ",";
        private String collStart = "{  \"Addresses\":[  \n";
        private String child1 = "         {  \n"
            + "            \"value\":{  \n"
            + "               \"Address\":\"address1\",\n"
            + "               \"Notes\": {\n"
            + "                   \"Note1\": \"someNote11\",\n"
            + "                   \"Note2\": \"someNote21\"\n"
            + "                }"
            + "            },\n"
            + "            \"id\":\"" + FIRST_CHILD_ID + "\"\n"
            + "         }\n";
        private String child1Updated = "         {  \n"
            + "            \"value\":{  \n"
            + "               \"Address\":\"address1\",\n"
            + "               \"Notes\": {\n"
            + "                   \"Note1\": \"someNote11 Updated\",\n"
            + "                   \"Note2\": \"someNote21 Updated\"\n"
            + "                }"
            + "            },\n"
            + "            \"id\":\"" + FIRST_CHILD_ID + "\"\n"
            + "         }\n";
        private String child2 = "         {  \n"
            + "            \"value\":{  \n"
            + "               \"Address\":\"address1\",\n"
            + "               \"Notes\": {\n"
            + "                   \"Note1\": \"someNote21\",\n"
            + "                   \"Note2\": \"someNote22\"\n"
            + "                }"
            + "            },\n"
            + "            \"id\":\"" + SECOND_CHILD_ID + "\"\n"
            + "         }\n";
        private String newChild = "         {  \n"
            + "            \"value\":{  \n"
            + "               \"Address\":\"address3\",\n"
            + "               \"Notes\": {\n"
            + "                   \"Note1\": \"someNote31\",\n"
            + "                   \"Note2\": \"someNote32\"\n"
            + "                }"
            + "            },\n"
            + "            \"id\":\"null\"\n"
            + "         }\n";
        private String newChildWithNoIdTag = "         {  \n"
            + "            \"value\":{  \n"
            + "               \"Address\":\"address3\",\n"
            + "               \"Notes\": {\n"
            + "                   \"Note1\": \"someNote31\",\n"
            + "                   \"Note2\": \"someNote32\"\n"
            + "                }"
            + "            }\n"
            + "         }\n";

        private String collEnd = "      ]\n }\n";

        private CaseFieldDefinition addressField;
        private CaseTypeDefinition caseType;

        @BeforeEach
        void setUp() throws IOException {
            existingDataNode = getJsonNode(collStart + child1 + comma + child2 + collEnd);

            addressField = newCaseField()
                .withId("Addresses")
                .withFieldType(aFieldType().withType(COLLECTION).build())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build())
                .build();
            addressField.getFieldTypeDefinition().setCollectionFieldTypeDefinition(getSimpleAddressFieldType());

            caseType = newCaseType()
                .withField(addressField)
                .build();
        }

        private FieldTypeDefinition getSimpleAddressFieldType() {
            return aFieldType()
                .withId("Address")
                .withType(COMPLEX)
                .withComplexField(newCaseField()
                    .withId("Address")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .build())
                .withComplexField(getNotesFieldDefinition())
                .build();
        }

        private CaseFieldDefinition getNotesFieldDefinition() {
            return newCaseField()
                .withId("Notes")
                .withFieldType(aFieldType()
                    .withId("NotesType")
                    .withType(COMPLEX)
                    .withComplexField(newCaseField()
                        .withId("Note1")
                        .withFieldType(aFieldType()
                            .withId("Text")
                            .withType("Text")
                            .build())
                        .build())
                    .withComplexField(newCaseField()
                        .withId("Note2")
                        .withFieldType(aFieldType()
                            .withId("Text")
                            .withType("Text")
                            .build())
                        .build())
                    .build())
                .build();
        }

        @Test
        @DisplayName("Should fail if the caseField not found")
        void shouldFailIfCaseFieldDoesNotExist() throws IOException {
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + comma + child2 + comma + newChild + collEnd),
                    existingDataNode,
                    Collections.emptyList(),
                    USER_ROLES),
                is(false));
        }

        @Test
        @DisplayName("Should allow creation of new items on collection")
        void shouldGrantCreateAccessToCollectionType() throws IOException {
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + comma + child2 + comma + newChild + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES),
                is(true));
        }

        @Test
        @DisplayName("Should allow creation of new items on collection even when no Id provided")
        void shouldGrantCreateAccessToCollectionTypeWOutId() throws IOException {
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + comma + child2 + comma + newChildWithNoIdTag + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES),
                is(true));
        }

        @Test
        @DisplayName("Should not allow creation of new items on collection")
        void shouldNotGrantCreateAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(asList(anAcl().withRole(ROLE_IN_USER_ROLES).withCreate(false).build()));
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + comma + child2 + comma + newChild + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES),
                is(false));
        }


        @Test
        @DisplayName("Should allow update of items on collection")
        void shouldGrantUpdateAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(asList(anAcl().withRole(ROLE_IN_USER_ROLES).withUpdate(true).build()));
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1Updated + comma + child2 + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES),
                is(true));
        }

        @Test
        @DisplayName("Should allow update of items on collection along with creation")
        void shouldGrantUpdateAndCreateAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(asList(anAcl().withRole(ROLE_IN_USER_ROLES).withCreate(true).withUpdate(true).build()));
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1Updated + comma + child2 + comma + newChildWithNoIdTag
                        + comma + newChild + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES),
                is(true));
        }

        @Test
        @DisplayName("Should not allow update of items on collection")
        void shouldNotGrantUpdateAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(asList(anAcl().withRole(ROLE_IN_USER_ROLES).withUpdate(false).build()));
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1Updated + comma + child2 + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES),
                is(false));
        }

        @Test
        @DisplayName("Should allow deletion of items on collection")
        void shouldGrantDeleteAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(asList(anAcl().withRole(ROLE_IN_USER_ROLES).withDelete(true).build()));
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES),
                is(true));
        }

        @Test
        @DisplayName("Should allow deletion of items on collection along with creation")
        void shouldGrantDeleteAndCreateAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(asList(anAcl().withRole(ROLE_IN_USER_ROLES).withCreate(true).withDelete(true).build()));
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + comma + newChildWithNoIdTag + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES),
                is(true));
        }

        @Test
        @DisplayName("Should not allow deletion of items on collection")
        void shouldNotGrantDeleteAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(asList(anAcl().withRole(ROLE_IN_USER_ROLES).withDelete(false).build()));
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES),
                is(false));
        }


        @Test
        @DisplayName("Should allow creation, updating and deletion of items on collection")
        void shouldGrantUpdateDeleteAndCreateAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(asList(anAcl().withRole(ROLE_IN_USER_ROLES).withCreate(true).withUpdate(true).withDelete(true).build()));
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1Updated + comma + child2 + comma
                        + newChildWithNoIdTag + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    USER_ROLES),
                is(true));
        }
    }

    private JsonNode getJsonNode(String content) throws IOException {
        final Map<String, JsonNode> newData = JacksonUtils.convertValue(MAPPER.readTree(content));
        return JacksonUtils.convertValueJsonNode(newData);
    }

    private void assertFieldsAccess(boolean hasFieldAccess, CaseTypeDefinition caseType, JsonNode newDataNode, JsonNode existingDataNode) {
        assertThat(
            accessControlService.canAccessCaseFieldsForUpsert(
                newDataNode,
                existingDataNode,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES),
            is(hasFieldAccess));
    }

    private JsonNode getTextNode(String value) {
        return JSON_NODE_FACTORY.textNode(value);
    }

    static CaseFieldDefinition getPeopleCollectionFieldDefinition() {
        CaseFieldDefinition caseField = newCaseField()
            .withId("People")
            .withFieldType(aFieldType()
                .withId("G339483948")
                .withType(COLLECTION)
                .build())
            .build();
        caseField.getFieldTypeDefinition().setCollectionFieldTypeDefinition(getPersonFieldType());
        return caseField;
    }

    static FieldTypeDefinition getPersonFieldType() {
        return aFieldType()
            .withId("Person")
            .withType(COMPLEX)
            .withComplexField(newCaseField()
                .withId("FirstName")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build())
            .withComplexField(newCaseField()
                .withId("LastName")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build())
            .withComplexField(getBirtInfoComplexField())
            .withComplexField(getAddressesCollectionFieldDefinition())
            .withComplexField(getNotesCollectionFieldDefinition())
            .build();
    }

    static CaseFieldDefinition getAddressesCollectionFieldDefinition() {
        CaseFieldDefinition caseField = newCaseField()
            .withId("Addresses")
            .withFieldType(aFieldType()
                .withId("Addresses-XYZT")
                .withType(COLLECTION)
                .build())
            .build();
        caseField.getFieldTypeDefinition().setCollectionFieldTypeDefinition(getAddressFieldType());
        return caseField;
    }

    static CaseFieldDefinition getBirtInfoComplexField() {
        return newCaseField()
            .withId("BirthInfo")
            .withFieldType(aFieldType()
                .withId("BirthInfoType")
                .withType(COMPLEX)
                .withComplexField(newCaseField()
                    .withId("BornCity")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .build())
                .withComplexField(newCaseField()
                    .withId("BornCountry")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .build())
                .withComplexField(newCaseField()
                    .withId("BornAddress")
                    .withFieldType(getAddressFieldType())
                    .build())
                .build())
            .build();
    }

    static FieldTypeDefinition getAddressFieldType() {
        return aFieldType()
            .withId("AddressComplexType")
            .withType(COMPLEX)
            .withComplexField(newCaseField()
                .withId("Name")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build())
            .withComplexField(newCaseField()
                .withId("Address")
                .withFieldType(getAddressDetailFieldType())
                .build())
            .build();
    }

    static FieldTypeDefinition getAddressDetailFieldType() {
        return aFieldType()
            .withId("AddressDetailComplexType")
            .withType(COMPLEX)
            .withComplexField(newCaseField()
                .withId("Line1")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build())
            .withComplexField(newCaseField()
                .withId("Line2")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build())
            .withComplexField(newCaseField()
                .withId("PostCode")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build())
            .withComplexField(newCaseField()
                .withId("Country")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build())
            .build();
    }

    static CaseFieldDefinition getNotesCollectionFieldDefinition() {
        CaseFieldDefinition notes = newCaseField()
            .withId("Notes")
            .withFieldType(aFieldType()
                .withId("Notes-EREJRKf")
                .withType(COLLECTION)
                .build())
            .build();
        notes.getFieldTypeDefinition().setCollectionFieldTypeDefinition(aFieldType()
            .withId("Note")
            .withType(COMPLEX)
            .withComplexField(newCaseField()
                .withId("Txt")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build())
            .withComplexField(getTagFieldDefinition())
            .build());
        return notes;
    }

    static CaseFieldDefinition getTagFieldDefinition() {
        CaseFieldDefinition tagsField = newCaseField()
            .withId("Tags")
            .withFieldType(aFieldType()
                .withId("Tag-EREJRKf")
                .withType(COLLECTION)
                .build())
            .build();
        tagsField.getFieldTypeDefinition().setCollectionFieldTypeDefinition(aFieldType()
            .withId("TagComplex")
            .withType(COMPLEX)
            .withComplexField(newCaseField()
                .withId("Tag")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build())
            .withComplexField(newCaseField()
                .withId("Category")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build())
            .build());
        return tagsField;
    }

    static JsonNode generatePeopleData() throws IOException {
        final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
            "{\n"
                + "  \"People\": [\n"
                    + person1
                + "    ,\n"
                    + person2
                + "  ]\n"
                + "}"
        ));

        return JacksonUtils.convertValueJsonNode(data);
    }
}
