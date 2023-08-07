package uk.gov.hmcts.ccd.domain.service.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.MANDATORY;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.OPTIONAL;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.READONLY;
import static uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinitionTest.findNestedField;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DOCUMENT;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_ROLE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.extractAccessProfileNames;
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

@SuppressWarnings("checkstyle:TypeName") // too many legacy TypeName occurrences on '@Nested' classes
public class AccessControlServiceTest {

    @Mock
    private ApplicationParams applicationParams;

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String EVENT_ID_WITH_ACCESS = "EVENT_ID_WITH_ACCESS";
    private static final String EVENT_ID_WITHOUT_ACCESS = "EVENT_ID_WITHOUT_ACCESS";
    private static final String EVENT_ID_WITHOUT_ACCESS_2 = "EVENT_ID_WITHOUT_ACCESS_2";
    private static final String EVENT_ID_WITH_ACCESS_2 = "EVENT_ID_WITH_ACCESS_2";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    static final String ROLE_IN_USER_ROLES = "caseworker-probate-loa1";
    static final String ROLE_IN_USER_ROLES_2 = "caseworker-divorce-loa";
    static final String ROLE_IN_USER_ROLES_3 = "caseworker-probate-loa3";
    static final String ROLE_NOT_IN_USER_ROLES = "caseworker-divorce-loa4";
    static final String ROLE_NOT_IN_USER_ROLES_2 = "caseworker-divorce-loa5";
    private static final String FIRST_CHILD_ID = "46f98326-6c88-426d-82be-d362f0246b7a";
    private static final String SECOND_CHILD_ID = "7c7cfd2a-b5d7-420a-8420-3ac3019cfdc7";
    static final Set<AccessProfile> ACCESS_PROFILES = createAccessProfiles(Sets.newHashSet(ROLE_IN_USER_ROLES,
        ROLE_IN_USER_ROLES_3,
        ROLE_IN_USER_ROLES_2));

    private AccessControlService accessControlService;
    private static final String CASE_TYPE_ID = "CASE_TYPE_ID";
    private static final String CASE_REFERENCE = "CASE_REFERENCE";
    private static final String EVENT_ID = "EVENT_ID";
    private static final String EVENT_ID_LOWER_CASE = "event_id";
    private static final String STATE_ID1 = "State1";
    private static final String STATE_ID2 = "State2";

    private static final String ADDRESSES = "Addresses";
    private static final String LINE1 = "Line1";
    private static final String LINE2 = "Line2";
    private static final String LINE3 = "Line3";

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
    static final String person2 =
        p2Start + p2Names + addressesStart + p2Address1 + "," + p2Address2 + arrayEnd + p2Notes + p2End;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;
    private List<ILoggingEvent> loggingEventList;
    private boolean logServiceClass;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accessControlService = new AccessControlServiceImpl(applicationParams, new CompoundAccessControlService());
    }

    @Nested
    @DisplayName("ACL tests - CaseStateDefinition")
    class CanAccessCaseStateWithCriteriaAclTests {

        @Test
        @DisplayName("Should not grant access to case state for user with missing role")
        void shouldNotGrantAccessToStateForUserWithMissingRole() {
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

            setupLogging().setLevel(Level.DEBUG);
            assertAll(
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID1, caseType,
                    ACCESS_PROFILES,
                    CAN_CREATE), is(false)),
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID2, caseType,
                    ACCESS_PROFILES,
                    CAN_CREATE), is(false))
            );
            String expectedLogMessage = TestBuildersUtil.formatLogMessage(
                    NO_ROLE_FOUND, "caseState", STATE_ID1,
                    extractAccessProfileNames(ACCESS_PROFILES),
                    "caseStateACL", "[ACL{accessProfile='caseworker-divorce-loa4', crud=CR}]"
            );
            loggingEventList = listAppender.list;
            assertAll(
                    () -> assertTrue(loggingEventList.stream().allMatch(log -> log.getLevel() == Level.DEBUG)),
                    () -> assertTrue(loggingEventList.stream().anyMatch(log ->
                            log.getFormattedMessage().equals(expectedLogMessage)))
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
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID1,
                    caseType, ACCESS_PROFILES,
                    CAN_CREATE), is(false)),
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID2,
                    caseType, ACCESS_PROFILES,
                    CAN_CREATE), is(false))
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
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID1,
                    caseType, ACCESS_PROFILES,
                    CAN_CREATE), is(true)),
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID2,
                    caseType, ACCESS_PROFILES,
                    CAN_CREATE), is(true))
            );
        }

        @Test
        @DisplayName("Shouldn't grant access to state when state is not present in definition")
        void shouldNotGrantAccessToStateIfStateIsNotPresentInDefinition() {
            CaseTypeDefinition caseType = newCaseType().build();

            assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID1, caseType, ACCESS_PROFILES,
                CAN_CREATE), is(false));
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
            CaseTypeDefinition caseTypeDefinition = newCaseType()
                .withState(caseState1)
                .withState(caseState2)
                .build();
            final List<CaseStateDefinition> states = accessControlService.filterCaseStatesByAccess(caseTypeDefinition,
                ACCESS_PROFILES, CAN_READ);

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
            CaseTypeDefinition caseTypeDefinition = newCaseType()
                .withState(caseState1)
                .withState(caseState2)
                .withState(caseState3)
                .build();
            final List<CaseStateDefinition> states =
                accessControlService.filterCaseStatesByAccess(caseTypeDefinition, ACCESS_PROFILES, CAN_READ);

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
    class CanAccessCaseFieldsWithCriteriaAclTests {

        @Test
        @DisplayName("Should fail to grant access to fields if acls are missing")
        void shouldFailToGrantCreateAccessForGivenFieldsIfOneFieldIsMissingAcls() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to case fields for user with missing role")
        void shouldNotGrantAccessToFieldsForUserWithMissingRole() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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

            setupLogging().setLevel(Level.ERROR);
            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFieldDefinitions(),
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));

            String expectedLogMessage = TestBuildersUtil.formatLogMessage(
                    NO_ROLE_FOUND, "caseField", ADDRESSES,
                    extractAccessProfileNames(ACCESS_PROFILES), "caseFieldACL",
                    "[ACL{accessProfile='caseworker-divorce-loa4', crud=CR}]"
            );

            loggingEventList = listAppender.list;
            assertAll(
                    () -> assertThat(loggingEventList.get(0).getLevel(), is(Level.ERROR)),
                    () -> assertThat(loggingEventList.get(0).getFormattedMessage(), is(expectedLogMessage))
            );
        }

        @Test
        @DisplayName("Should not grant access to case fields with relevant acl not granting access")
        void shouldNotGrantAccessToFieldsIfRelevantAclNotGrantingAccess() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to case fields if ACL false and null value")
        void shouldNotGrantAccessToNullValue() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should grant access to case fields with acl matching")
        void shouldGrantAccessToFieldsWithAclMatching() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(true));
        }

        @Test
        @DisplayName("Should not grant access to case fields when field is not present in definition")
        void shouldGrantAccessToFieldsIfFieldNotPresentInDefinition() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                    .withField(newCaseField()
                            .withId(ADDRESSES)
                            .build())
                    .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                    "{  \"WrongAddresses\": \"someText\" }"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            setupLogging().setLevel(Level.ERROR);
            assertThat(
                    accessControlService.canAccessCaseFieldsWithCriteria(
                            dataNode,
                            caseType.getCaseFieldDefinitions(),
                            ACCESS_PROFILES,
                            CAN_CREATE),
                    is(false));

            String expectedLogMessage = TestBuildersUtil.formatLogMessage(
                    "No matching caseField={} in caseFieldDefinitions", "WrongAddresses");

            loggingEventList = listAppender.list;
            assertAll(
                    () -> assertThat(loggingEventList.get(0).getLevel(), is(Level.ERROR)),
                    () -> assertThat(loggingEventList.get(0).getFormattedMessage(), is(expectedLogMessage))
            );
        }

        @Test
        @DisplayName("Should grant access to case fields when expected fields is empty in definition file")
        void shouldGrantAccessToFieldsIfFieldNotPresentInDefinitionAndDefinitionIsEmpty() throws IOException {
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(true));
        }
    }

    @Nested
    @DisplayName("text value tests")
    class CanAccessCaseFieldsWithCriteriaTextValueTypeTests {

        @Test
        @DisplayName("Should grant access to case fields with text value")
        void shouldGrantAccessToTextValueType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(true));
        }

        @Test
        @DisplayName("Should not grant access to case fields if ACL false and empty text")
        void shouldNotGrantAccessToEmptyTextType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));
        }
    }

    @Nested
    @DisplayName("collection value tests")
    class CanAccessCaseFieldsWithCriteriaCollectionValueTypeTests {

        @Test
        @DisplayName("Should grant access to case fields with collection")
        void shouldGrantAccessToCollectionType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(true));
        }

        @Test
        @DisplayName("Should not grant access to case fields if ACL false and empty collection")
        void shouldNotGrantCreateAccessToCollectionTypeIfEmpty() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));
        }
    }

    @Nested
    @DisplayName("complex value tests")
    class CanAccessCaseFieldsWithCriteriaComplexValueTypeTests {

        @Test
        @DisplayName("Should grant access to case fields with complex object")
        void shouldGrantAccessToComplexType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(true));
        }

        @Test
        @DisplayName("Should not grant access to case fields if ACL false and empty object")
        void shouldNotGrantAccessToComplexTypeIfEmpty() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));
        }
    }

    @Nested
    @DisplayName("case fields upsert ACL tests")
    class CanAccessCaseFieldsForUpsertAclTests {

        @Test
        @DisplayName("Should not grant access to field if field acls are missing for update")
        void shouldNotGrantAccessToFieldsIfFieldIsMissingAclsForUpdate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
                    .withFieldType(aFieldType().withType(TEXT).build())
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
                    .withId(ADDRESSES)
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
                    .withId(ADDRESSES)
                    .withFieldType(aFieldType().withType(TEXT).build())
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
                    .withId(ADDRESSES)
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
                    .withId(ADDRESSES)
                    .withFieldType(aFieldType().withType(TEXT).build())
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
                    .withId(ADDRESSES)
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
                    .withId(ADDRESSES)
                    .withFieldType(aFieldType().withType(TEXT).build())
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
                    .withId(ADDRESSES)
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
                    .withId(ADDRESSES)
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
        @DisplayName("Should not grant access to case field if ACL false for collection of Document Type")
        void shouldNotGrantAccessToFieldWithAclAccessNotGrantedForCollectionOfDocuments() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Documents")
                    .withFieldType(aFieldType()
                        .withType(COLLECTION)
                        .withCollectionFieldType(aFieldType()
                            .withType(DOCUMENT)
                            .withId(DOCUMENT)
                            .build())
                        .build())
                    .withOrder(1)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(false)
                        .withUpdate(false)
                        .withDelete(false)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            JsonNode newDataNode = getJsonNode("{\n"
                + "  \"Documents\": [\n"
                + "    {\n"
                + "      \"id\": \"CollectionField1\",\n"
                + "      \"value\": {\n"
                + "        \"document_url\": \"{{DM_STORE_BASE_URL}}/documents/"
                + "ae5c9e4b-1385-483e-b1b7-607e75dd3943\",\n"
                + "        \"document_binary_url\": \"{{DM_STORE_BASE_URL}}/documents/"
                + "ae5c9e4b-1385-483e-b1b7-607e75dd3943/binary\",\n"
                + "        \"document_filename\": \"Elastic Search test Case.png --> updated by Solicitor 1\"\n"
                + "      }\n"
                + "    },\n"
                + "    {\n"
                + "      \"id\": \"CollectionField2\",\n"
                + "      \"value\": {\n"
                + "        \"document_url\": \"{{DM_STORE_BASE_URL}}/documents/"
                + "ae5c9e4b-1385-483e-b1b7-607e75dd3943\",\n"
                + "        \"document_binary_url\": \"{{DM_STORE_BASE_URL}}/documents/"
                + "ae5c9e4b-1385-483e-b1b7-607e75dd3943/binary\",\n"
                + "        \"document_filename\": \"Elastic Search test Case.png --> updated by Solicitor 1\"\n"
                + "      }\n"
                + "    }\n"
                + "  ]\n"
                + "}");
            JsonNode existingDataNode = getJsonNode("{\n"
                + "  \"Documents\": [\n"
                + "    {\n"
                + "      \"id\": \"CollectionField1\",\n"
                + "      \"value\": {\n"
                + "        \"document_url\": \"{{DM_STORE_BASE_URL}}/documents/"
                + "ae5c9e4b-1385-483e-b1b7-607e75yfhgfhg\",\n"
                + "        \"document_binary_url\": \"{{DM_STORE_BASE_URL}}/documents/"
                + "ae5c9e4b-1385-483e-b1b7-607e75yfhgfhg/binary\",\n"
                + "        \"document_filename\": \"Elastic Search test Case.png --> updated by Solicitor 1\"\n"
                + "      }\n"
                + "    },\n"
                + "    {\n"
                + "      \"id\": \"CollectionField2\",\n"
                + "      \"value\": {\n"
                + "        \"document_url\": \"{{DM_STORE_BASE_URL}}/documents/"
                + "ae5c9e4b-1385-483e-b1b7-607e75dd3943\",\n"
                + "        \"document_binary_url\": \"{{DM_STORE_BASE_URL}}/documents/"
                + "ae5c9e4b-1385-483e-b1b7-607e75dd3943/binary\",\n"
                + "        \"document_filename\": \"Elastic Search test Case.png --> updated by Solicitor 1\"\n"
                + "      }\n"
                + "    }\n"
                + "  ]\n"
                + "}");
            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to case field if ACL true and field name not matching for create")
        void shouldNotGrantAccessToFieldWithAclAccessGrantedAndFieldNameNotMatchingForCreate() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                    .withId(ADDRESSES)
                    .withFieldType(aFieldType().withType(TEXT).build())
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
                    .withId(ADDRESSES)
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
                    .withId(ADDRESSES)
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
                    .withId(ADDRESSES)
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("FirstName")
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("LastName")
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Mobile")
                    .withFieldType(aFieldType().withType(TEXT).build())
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
                    .withId(ADDRESSES)
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("FirstName")
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("LastName")
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Mobile")
                    .withFieldType(aFieldType().withType(TEXT).build())
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
                    .withId(ADDRESSES)
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("FirstName")
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("LastName")
                    .withFieldType(aFieldType().withType(TEXT).build())
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
    class CanAccessCaseEventWithCriteriaAclTests {

        @Test
        @DisplayName("Should not grant access to event if acls are missing")
        void shouldNotGrantAccessToEventIfEventIsMissingAcls() {
            final CaseTypeDefinition caseType = new CaseTypeDefinition();
            CaseEventDefinition eventDefinition = new CaseEventDefinition();
            eventDefinition.setId(EVENT_ID);
            caseType.setEvents(singletonList(eventDefinition));

            assertThat(
                accessControlService.canAccessCaseEventWithCriteria(
                    EVENT_ID,
                    caseType.getEvents(),
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to event for user with missing role")
        void shouldNotGrantAccessToEventForUserWithMissingRole() {
            final CaseTypeDefinition caseType = new CaseTypeDefinition();
            CaseEventDefinition eventDefinition = new CaseEventDefinition();
            eventDefinition.setId(EVENT_ID);
            AccessControlList accessControlList = new AccessControlList();
            accessControlList.setAccessProfile(ROLE_NOT_IN_USER_ROLES);
            accessControlList.setCreate(true);
            List<AccessControlList> accessControlLists = newArrayList(accessControlList);
            eventDefinition.setAccessControlLists(accessControlLists);
            caseType.setEvents(singletonList(eventDefinition));

            logServiceClass = true;
            setupLogging().setLevel(Level.ERROR);
            assertThat(
                accessControlService.canAccessCaseEventWithCriteria(
                    EVENT_ID,
                    caseType.getEvents(),
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));

            loggingEventList = listAppender.list;
            String expectedLogMessage = TestBuildersUtil.formatLogMessage(
                    NO_ROLE_FOUND, "caseEvent", EVENT_ID,
                    extractAccessProfileNames(ACCESS_PROFILES),
                    "caseEventACL", "[ACL{accessProfile='caseworker-divorce-loa4', crud=C}]");
            assertAll(
                    () -> assertThat(loggingEventList.get(0).getLevel(), is(Level.ERROR)),
                    () -> assertThat(loggingEventList.get(0).getFormattedMessage(), is(expectedLogMessage))
            );
        }

        @Test
        @DisplayName("Should not grant access to event with relevant acl not granting access")
        void shouldNotGrantAccessToEventIfRelevantAclNotGrantingAccess() {
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to event if ACL false and null value")
        void shouldNotGrantAccessToNullValue() {
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
                    ACCESS_PROFILES,
                    AccessControlList::isCreate),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to event if ACL true and event name not matching")
        void shouldNotGrantAccessWithEventNameNotMatching() {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            logServiceClass = true;
            setupLogging().setLevel(Level.ERROR);
            assertThat(
                accessControlService.canAccessCaseEventWithCriteria(
                    EVENT_ID_LOWER_CASE,
                    caseType.getEvents(),
                    ACCESS_PROFILES,
                    AccessControlList::isCreate),
                is(false));

            String expectedLogMessage = TestBuildersUtil.formatLogMessage(
                    "No matching caseEvent={} in caseEventDefinitions",
                        EVENT_ID_LOWER_CASE
            );

            loggingEventList = listAppender.list;
            assertAll(
                    () -> assertThat(loggingEventList.get(0).getLevel(), is(Level.ERROR)),
                    () -> assertThat(loggingEventList.get(0).getFormattedMessage(), is(expectedLogMessage))
            );
        }

        @Test
        @DisplayName("Should grant access to event with acl matching")
        void shouldGrantAccessToEventWithAclMatching() {
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(true));
        }
    }

    @Nested
    @DisplayName("case ACL tests")
    class CanAccessCaseTypeWithCriteriaAclTests {

        @Test
        @DisplayName("Should not grant access to case if acls are missing")
        void shouldNotGrantAccessToCaseIfMissingAcls() {
            final CaseTypeDefinition caseType = newCaseType().build();

            assertThat(
                accessControlService.canAccessCaseTypeWithCriteria(
                    caseType,
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to case for user with missing role")
        void shouldNotGrantAccessToCaseForUserWithMissingRole() {
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to case with relevant acl not granting access")
        void shouldNotGrantAccessToCaseIfRelevantAclNotGrantingAccess() {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();

            setupLogging().setLevel(Level.DEBUG);
            assertThat(
                accessControlService.canAccessCaseTypeWithCriteria(
                    caseType,
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(false));

            loggingEventList = listAppender.list;
            String expectedLogMessage = TestBuildersUtil.formatLogMessage(
                    NO_ROLE_FOUND, "caseType", caseType.getId(),
                    "[caseworker-divorce-loa, caseworker-probate-loa3, caseworker-probate-loa1]",
                    "caseTypeACL", List.of());

            assertAll(
                    () -> assertThat(loggingEventList.get(0).getLevel(), is(Level.DEBUG)),
                    () -> assertThat(loggingEventList.get(0).getFormattedMessage(), is(expectedLogMessage))
            );

        }

        @Test
        @DisplayName("Should not grant access to case if caseType is null")
        void shouldNotGrantAccessToCaseIfCaseTypeIsNull() {
            assertThat(
                    accessControlService.canAccessCaseTypeWithCriteria(
                            null,
                            ACCESS_PROFILES,
                            CAN_CREATE),
                    is(false));
        }

        @Test
        @DisplayName("Should grant access to case with acl matching")
        void shouldGrantAccessToCaseWithAclMatching() {
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
                    ACCESS_PROFILES,
                    CAN_CREATE),
                is(true));
        }
    }

    @Nested
    @DisplayName("case fields ACL tests")
    class ReturnsDataWithCaseFieldReadAccessAclTests {

        @Test
        @DisplayName("Should not return data if field acls are missing")
        void shouldNotReturnDataIfCaseFieldIsMissingAcls() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
                    .build())
                .build();
            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"SomeText\" }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should not return data for user with missing role")
        void shouldNotReturnFieldForUserWithMissingRole() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should not return data if field with relevant acl not granting access")
        void shouldNotGrantAccessToFieldsIfRelevantAclNotGrantingAccess() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should not return data if field with acl false and null value")
        void shouldNotReturnDataWithAclFalseAndNullValue() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should not return data if field with acl true and field name not matching")
        void shouldNotReturnDataWithAclTrueAndFieldNameNotMatching() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should return data if field with acl true and null value")
        void shouldReturnDataWithAclTrueAndNullValue() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(dataNode)));
        }

        @Test
        @DisplayName("Should return data if field with acl true and empty value")
        void shouldReturnDataWithAclTrueAndEmptyValue() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(dataNode)));
        }

        @Test
        @DisplayName("Should return data if field with acl matching")
        void shouldGrantAccessToFieldsWithAclMatching() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(dataNode)));
        }
    }

    @Nested
    @DisplayName("return fields data with text value tests")
    class ReturnsDataWithCaseFieldAccessTextValueTypeTests {

        @Test
        @DisplayName("Should not return data if field ACL false and empty text")
        void shouldNotGrantAccessToEmptyTextType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should return data with null and empty values on root level")
        void shouldReturnDataWithNullAndEmptyValuesOnRootLevel() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertAll(
                () -> assertThat(jsonNode.get(ADDRESSES), is(JSON_NODE_FACTORY.nullNode())),
                () -> assertThat(jsonNode.get("Addresses2"), is(getTextNode("")))
            );
        }
    }

    @Nested
    @DisplayName("return fields data with collection value tests")
    class ReturnsDataWithCaseFieldAccessCollectionValueTypeTests {
        private static final String VALUE = "value";
        private static final String ID = "id";

        @Test
        @DisplayName("Should return data if field with collection")
        void shouldGrantAccessToCollectionType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(dataNode)));
        }

        @Test
        @DisplayName("Should return data if field and children have ACLs")
        void shouldGrantAccessToCollectionTypeChildren() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(singletonList(anAcl()
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
                    .withListElementCode(ADDRESSES)
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
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            JsonNode dataNode = generatePeopleData();

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertAll(
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("FirstName").textValue(), is("Fatih")),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("LastName"), is(nullValue())),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get(ADDRESSES).get(0).get("value")
                    .get("Name").textValue(), is("home")),
                () -> assertThat(
                    jsonNode.get("People").get(0).get("value").get(ADDRESSES).get(1).get("value").get("Address")
                        .get(LINE1).textValue(),
                    is("41 Kings Road")
                ),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("Notes").get(0).get("value").get("Txt")
                    .textValue(), is("someNote11")),
                () -> assertThat(jsonNode.get("People").get(0).get("value").size(), is(3))
            );
        }

        @Test
        @DisplayName("Should filter data when child doesnot have ACLs")
        void shouldfilterDataWhenChildDoesnotHaveACL() throws IOException {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(singletonList(anAcl()
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
                    .withListElementCode(ADDRESSES)
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
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            JsonNode dataNode = generatePeopleData();

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertAll(
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("FirstName").textValue(), is("Fatih")),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("LastName"), is(nullValue())),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("BirthInfo").get("BornCity")
                    .textValue(), is("Salihli")),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("BirthInfo").get("BornCountry"),
                    is(nullValue())),
                () -> assertThat(
                    jsonNode.get("People").get(0).get("value").get("BirthInfo").get("BornAddress").get("Address")
                        .get(LINE1).textValue(),
                    is("23 Lampton Road")
                ),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get(ADDRESSES).get(0).get("value")
                    .get("Name").textValue(), is("home")),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get(ADDRESSES).get(0).get("value")
                    .get("Address"), is(nullValue())),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("Notes").get(0).get("value")
                    .get("Note"), is(nullValue())),
                () -> assertThat(jsonNode.get("People").get(0).get("value").get("Notes").get(0).get("value")
                    .get("Txt").textValue(), is("someNote11")),
                () -> assertThat(jsonNode.get("People").get(0).get("value").size(), is(4))
            );
        }

        @Test
        @DisplayName("Should filter data for missing node and return remaining data")
        void shouldFilterDataForMissingNodeAndReturnRemainingData() throws IOException {
            Logger logger = (Logger) LoggerFactory.getLogger(AccessControlService.class);
            ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
            listAppender.start();
            logger.addAppender(listAppender);

            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(singletonList(anAcl()
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
                    .withListElementCode(ADDRESSES)
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
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            JsonNode dataNode = generatePeopleData();

            ((  ObjectNode) dataNode.get("People").get(0).get(VALUE)).replace("BirthInfo", MissingNode.getInstance());

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertAll(() -> assertTrue(jsonNode.get("People").get(0).get(VALUE).get("BirthInfo").isEmpty()),
                () -> assertThat(jsonNode.get("People").get(0).get(VALUE).get("FirstName").textValue(), is("Fatih")),
                () -> assertThat(jsonNode.get("People").get(1).get(VALUE).get("FirstName").textValue(), is("Andrew")));

            List<ILoggingEvent> logsList = listAppender.list;
            assertEquals("Can not find field with caseFieldId=BirthInfo, "
                    + "accessControlList=[ACL{accessProfile='caseworker-probate-loa1', crud=R}]",
                logsList.get(0).getFormattedMessage());

            logger.detachAndStopAllAppenders();
        }

        @Test
        @DisplayName("Should return data with null and empty values on root level")
        void shouldReturnDataWithNullAndEmptyValuesOnRootLevel() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertAll(
                () -> assertThat(jsonNode.has("Addresses2"), is(true)),
                () -> assertThat(jsonNode.get("Addresses2"), is(JSON_NODE_FACTORY.arrayNode())),
                () -> assertThat(jsonNode.has("Addresses3"), is(true)),
                () -> assertThat(jsonNode.get("Addresses3"), is(JSON_NODE_FACTORY.nullNode())),
                () -> assertThat(jsonNode.get(ADDRESSES).get(0).get(ID), is(getTextNode(FIRST_CHILD_ID))),
                () -> assertThat(jsonNode.get(ADDRESSES).get(0).get(VALUE).get("Address"),
                    is(getTextNode("address1"))),
                () -> assertThat(jsonNode.get(ADDRESSES).get(0).get(VALUE).get("Notes").get("Note1"),
                    is(getTextNode("someNote11"))),
                () -> assertThat(jsonNode.get(ADDRESSES).get(0).get(VALUE).get("Notes").get("Note2"),
                    is(getTextNode("someNote21"))),
                () -> assertThat(jsonNode.get(ADDRESSES).get(1).get(ID), is(getTextNode(SECOND_CHILD_ID))),
                () -> assertThat(jsonNode.get(ADDRESSES).get(1).get(VALUE).get("Address"),
                    is(getTextNode("address2"))),
                () -> assertThat(jsonNode.get(ADDRESSES).get(1).get(VALUE).get("Notes").get("Note1"),
                    is(getTextNode("someNote21"))),
                () -> assertThat(jsonNode.get(ADDRESSES).get(1).get(VALUE).get("Notes").get("Note2"),
                    is(getTextNode("someNote22")))
            );
        }


        @Test
        @DisplayName("Should not return data if field ACL false and empty collection")
        void shouldNotGrantAccessToCollectionTypeIfEmpty() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }
    }

    @Nested
    @DisplayName("return fields data with complex value tests")
    class ReturnsDataWithCaseFieldReadAccessComplexValueTypeTests {

        @Test
        @DisplayName("Should return data if field with complex object")
        void shouldGrantAccessToComplexType() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode.get(ADDRESSES).get("Note"), is(getTextNode("someNote11")));
        }

        @Test
        @DisplayName("Should return data with null and empty values on root level")
        void shouldReturnDataWithNullAndEmptyValuesOnRootLevel() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertAll(
                () -> assertThat(jsonNode.get(ADDRESSES), is(JSON_NODE_FACTORY.nullNode())),
                () -> assertThat(jsonNode.get("Addresses2").get("Note"), is(getTextNode("")))
            );
        }

        @Test
        @DisplayName("Should not return data if field ACL false and empty object")
        void shouldNotGrantAccessToComplexTypeIfEmpty() throws IOException {
            CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();

            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"Addresses\":{} }\n"
            ));
            JsonNode dataNode = JacksonUtils.convertValueJsonNode(data);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_READ,
                false);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }
    }

    @Nested
    @DisplayName("case type ACL tests")
    class ReturnsDataWithCaseTypeAclTests {

        @Test
        @DisplayName("Should not return event if event is missing")
        void shouldNotReturnEventIfCaseEventIsMissing() {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .build())
                .build();

            assertThat(accessControlService.filterCaseAuditEventsByReadAccess(null,
                caseType.getEvents(),
                ACCESS_PROFILES),
                is(emptyCollectionOf(AuditEvent.class)));
        }
    }

    @Nested
    @DisplayName("case audit events ACL tests")
    class ReturnsDataWithCaseEventReadAccessAclTests {

        @Test
        @DisplayName("Should not return audit event if event is missing")
        void shouldNotReturnEventIfCaseEventIsMissing() {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .build())
                .build();

            assertThat(accessControlService.filterCaseAuditEventsByReadAccess(null,
                caseType.getEvents(),
                ACCESS_PROFILES),
                is(emptyCollectionOf(AuditEvent.class)));
        }

        @Test
        @DisplayName("Should not return audit event if acls are missing")
        void shouldNotReturnEventIfCaseEventIsMissingAcls() {
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
                ACCESS_PROFILES),
                is(emptyCollectionOf(AuditEvent.class)));
        }

        @Test
        @DisplayName("Should not return audit event for user with missing role")
        void shouldNotReturnEventForUserWithMissingRole() {
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
                ACCESS_PROFILES),
                is(emptyCollectionOf(AuditEvent.class)));
        }

        @Test
        @DisplayName("Should not return audit event if relevant acl not granting access")
        void shouldNotReturnEventIfRelevantAclNotGrantingAccess() {
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
                ACCESS_PROFILES),
                is(emptyCollectionOf(AuditEvent.class)));
        }

        @Test
        @DisplayName("Should not return audit event if ACL true and event name not matching")
        void shouldNotReturnEventIfRelevantAclGrantingAccessAndEventNameNotMatching() {
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
                ACCESS_PROFILES),
                is(emptyCollectionOf(AuditEvent.class)));
        }

        @Test
        @DisplayName("Should return audit event if acl matching")
        void shouldReturnEventWithAclMatching() {
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
                ACCESS_PROFILES),
                is(auditEvents));
        }

        @Test
        @DisplayName("Should return single audit event if acl matching from a group")
        void shouldReturnEventWithAclMatchingFromGroup() {
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
                ACCESS_PROFILES),
                is(auditEvents));
        }

        @Test
        @DisplayName("Should return audit events if acls matching")
        void shouldReturnEventsWithAclsMatching() {
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
                ACCESS_PROFILES);
            assertThat(actual, containsInAnyOrder(hasProperty("eventId", is("EVENT_ID_WITH_ACCESS")),
                hasProperty("eventId", is("EVENT_ID_WITH_ACCESS_2"))));
        }
    }


    @Nested
    @DisplayName("case event trigger ACL tests")
    class ReturnsCaseEventTriggerDataWithCaseFieldReadonlyAclTests {

        @Test
        @DisplayName("Should set readonly flag if relevant acl missing")
        void shouldSetReadonlyFlagIfRelevantAclMissing() {
            final CaseTypeDefinition caseType = defaultCaseTypeDefinition();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(1, eventTrigger.getCaseFields().size());
            assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext",
                is(READONLY))));
        }

        @Test
        @DisplayName("Should remove field if relevant acl missing with multiparty fix")
        void shouldRemoveFieldIfRelevantAclMissingWithMultipartyFix() {
            setApplicationParamsForTest();

            final CaseTypeDefinition caseType = defaultCaseTypeDefinition();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(0, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should remove field if relevant acl missing with multiparty fix for all events")
        void shouldRemoveFieldIfRelevantAclMissingWithMultipartyFixForAllEvents() {
            Mockito.when(applicationParams.isMultipartyFixEnabled()).thenReturn(true);
            Mockito.when(applicationParams.getMultipartyCaseTypes()).thenReturn(singletonList(CASE_TYPE_ID));
            Mockito.when(applicationParams.getMultipartyEvents()).thenReturn(singletonList("*"));

            final CaseTypeDefinition caseType = defaultCaseTypeDefinition();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(0, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should remove field if relevant acl missing with multiparty fix for all case types and events")
        void shouldRemoveFieldIfRelevantAclMissingWithMultipartyFixForAllCaseTypesAndEvents() {
            Mockito.when(applicationParams.isMultipartyFixEnabled()).thenReturn(true);
            Mockito.when(applicationParams.getMultipartyCaseTypes()).thenReturn(singletonList("*"));
            Mockito.when(applicationParams.getMultipartyEvents()).thenReturn(singletonList("*"));

            final CaseTypeDefinition caseType = defaultCaseTypeDefinition();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(0, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should remove field if relevant acl missing with multiparty fix for all case types")
        void shouldRemoveFieldIfRelevantAclMissingWithMultipartyFixForAllCaseTypesAndSpecificEvent() {
            Mockito.when(applicationParams.isMultipartyFixEnabled()).thenReturn(true);
            Mockito.when(applicationParams.getMultipartyCaseTypes()).thenReturn(singletonList("*"));
            Mockito.when(applicationParams.getMultipartyEvents()).thenReturn(singletonList(EVENT_ID));

            final CaseTypeDefinition caseType = defaultCaseTypeDefinition();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(0, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should remove field if relevant acl missing with multiparty fix for multiple events")
        void shouldRemoveFieldIfRelevantAclMissingWithMultipartyFixForMultipleEvents() {
            Mockito.when(applicationParams.isMultipartyFixEnabled()).thenReturn(true);
            Mockito.when(applicationParams.getMultipartyCaseTypes()).thenReturn(singletonList(CASE_TYPE_ID));
            Mockito.when(applicationParams.getMultipartyEvents()).thenReturn(asList(EVENT_ID, "VALID_EVENT"));

            final CaseTypeDefinition caseType = defaultCaseTypeDefinition();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(0, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should not remove field if relevant acl missing with multiparty fix for invalid event")
        void shouldNotRemoveFieldIfRelevantAclMissingWithMultipartyFixForInvalidEvent() {
            Mockito.when(applicationParams.isMultipartyFixEnabled()).thenReturn(true);
            Mockito.when(applicationParams.getMultipartyCaseTypes()).thenReturn(singletonList("*"));
            Mockito.when(applicationParams.getMultipartyEvents()).thenReturn(singletonList("INVALID_EVENT"));

            final CaseTypeDefinition caseType = defaultCaseTypeDefinition();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(1, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should not remove field if relevant acl missing with multiparty fix for invalid case type")
        void shouldNotRemoveFieldIfRelevantAclMissingWithMultipartyFixForInvalidCaseType() {
            Mockito.when(applicationParams.isMultipartyFixEnabled()).thenReturn(true);
            Mockito.when(applicationParams.getMultipartyCaseTypes()).thenReturn(singletonList("INVALID_CASE_TYPE"));
            Mockito.when(applicationParams.getMultipartyEvents()).thenReturn(singletonList("*"));

            final CaseTypeDefinition caseType = defaultCaseTypeDefinition();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(1, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should set readonly flag if relevant acl missing but has read access with multiparty fix")
        void shouldSetReadonlyFlagIfRelevantAclMissingButHasReadAccessWithMultipartyFix() {
            setApplicationParamsForTest();

            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withId(ADDRESSES)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(false)
                        .withRead(true)
                        .build())
                    .build())
                .build();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(1, eventTrigger.getCaseFields().size());
            assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext",
                is(READONLY))));
        }

        @Test
        @DisplayName("Should set readonly flag if relevant field acl missing")
        void shouldSetReadonlyFlagIfRelevantFieldACLMissing() {
            final CaseTypeDefinition caseType = defaultCaseTypeDefinition();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(1, eventTrigger.getCaseFields().size());
            assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext",
                is(READONLY))));
        }

        @Test
        @DisplayName("Should remove field if relevant field acl missing with multiparty fix")
        void shouldRemoveFieldIfRelevantFieldACLMissingWithMultipartyFix() {
            setApplicationParamsForTest();

            final CaseTypeDefinition caseType = defaultCaseTypeDefinition();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(0, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should set readonly flag for complex children if relevant acl missing")
        void shouldSetReadonlyFlagForComplexChildrenIfRelevantAclMissing() {
            final CaseTypeDefinition caseType = createCaseTypeWithTwoSubFields(TEXT);
            CaseUpdateViewEvent caseEventTrigger = createCaseUpdateViewEventWithTwoSubFields(TEXT);

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext",
                    is(READONLY)))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), LINE1),
                    hasProperty("displayContext", is(READONLY))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), LINE2),
                    hasProperty("displayContext", is(READONLY)))
            );
        }

        @Test
        @DisplayName("Should set readonly flag for complex children if relevant acl missing with multiparty fix")
        void shouldSetReadonlyFlagForComplexChildrenIfRelevantAclMissingWithMultipartyFix() {
            setApplicationParamsForTest();

            final CaseTypeDefinition caseType = createCaseTypeWithTwoSubFields(TEXT);
            CaseUpdateViewEvent caseEventTrigger = createCaseUpdateViewEventWithTwoSubFields(TEXT);

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext",
                    is(READONLY)))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), LINE1),
                    hasProperty("displayContext", is(READONLY))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), LINE2),
                    hasProperty("displayContext", is(READONLY)))
            );
        }

        @Test
        @DisplayName("Should remove complex children if relevant acl missing with multiparty fix")
        void shouldRemoveComplexChildrenIfRelevantAclMissingWithMultipartyFix() {
            setApplicationParamsForTest();

            final CaseTypeDefinition caseType = createCaseTypeWithThreeSubFields(TEXT);
            CaseUpdateViewEvent caseEventTrigger = createCaseUpdateViewEventWithThreeSubFields(TEXT);

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertAll(
                () -> assertEquals(1, eventTrigger.getCaseFields().size()),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField(LINE1).isPresent(),
                    is(true)),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField(LINE2).isPresent(),
                    is(false)),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField(LINE3).isPresent(),
                    is(false))
            );
        }

        @Test
        @DisplayName("Should set readonly flag for children of complex type if relevant acl missing")
        void shouldSetReadonlyFlagForChildrenOfComplexTypeIfRelevantAclMissing() {
            final CaseTypeDefinition caseType = createCaseTypeWithTwoSubFields(COMPLEX);
            CaseUpdateViewEvent caseEventTrigger = createCaseUpdateViewEventWithTwoSubFields(COMPLEX);

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext",
                    is(READONLY)))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), LINE1),
                    hasProperty("displayContext", is(READONLY))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), LINE2),
                    hasProperty("displayContext", is(READONLY)))
            );
        }

        @Test
        @DisplayName(
            "Should set readonly flag for children of complex type if relevant acl missing with multiparty fix")
        void shouldSetReadonlyFlagForChildrenOfComplexIfRelevantAclMissingWithMultipartyFix() {
            setApplicationParamsForTest();

            final CaseTypeDefinition caseType = createCaseTypeWithTwoSubFields(COMPLEX);
            CaseUpdateViewEvent caseEventTrigger = createCaseUpdateViewEventWithTwoSubFields(COMPLEX);

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext",
                    is(READONLY)))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), LINE1),
                    hasProperty("displayContext", is(READONLY))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), LINE2),
                    hasProperty("displayContext", is(READONLY)))
            );
        }

        @Test
        @DisplayName("Should remove children of complex type if relevant acl missing with multiparty fix")
        void shouldRemoveChildrenOfComplexTypeIfRelevantAclMissingWithMultipartyFix() {
            setApplicationParamsForTest();

            final CaseTypeDefinition caseType = createCaseTypeWithThreeSubFields(COMPLEX);
            CaseUpdateViewEvent caseEventTrigger = createCaseUpdateViewEventWithThreeSubFields(COMPLEX);

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertAll(
                () -> assertEquals(1, eventTrigger.getCaseFields().size()),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField(LINE1).isPresent(),
                    is(true)),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField(LINE2).isPresent(),
                    is(false)),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField(LINE3).isPresent(),
                    is(false))
            );
        }

        @Test
        @DisplayName("Should remove complex parent with children if relevant acl missing with multiparty fix")
        void shouldRemoveComplexParentWithChildrenIfRelevantAclMissingWithMultipartyFix() {
            setApplicationParamsForTest();

            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType()
                        .withType(COMPLEX)
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE1)
                                .build())
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE2)
                                .build())
                        .build())
                    .withId(ADDRESSES)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(false)
                        .withRead(false)
                        .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode(LINE1)
                            .withRole(ROLE_IN_USER_ROLES)
                            .withCreate(false)
                            .withUpdate(false)
                            .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode(LINE2)
                            .withRole(ROLE_IN_USER_ROLES)
                            .withCreate(true)
                            .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            CaseUpdateViewEvent caseEventTrigger = createCaseUpdateViewEventWithTwoSubFields(TEXT);

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(0, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should remove multiple complex parent with children if relevant acl missing with multiparty fix")
        void shouldRemoveMultipleComplexParentWithChildrenIfRelevantAclMissingWithMultipartyFix() {
            setApplicationParamsForTest();

            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType()
                        .withType(COMPLEX)
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE1)
                                .build())
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE2)
                                .build())
                        .build())
                    .withId("ResidenceAddress")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(false)
                        .withRead(false)
                        .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode(LINE1)
                            .withRole(ROLE_IN_USER_ROLES)
                            .withCreate(false)
                            .withUpdate(false)
                            .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode(LINE2)
                            .withRole(ROLE_IN_USER_ROLES)
                            .withCreate(true)
                            .build())
                    .build())
                .withField(newCaseField()
                    .withFieldType(aFieldType()
                        .withType(COMPLEX)
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE1)
                                .build())
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE2)
                                .build())
                        .build())
                    .withId("OfficeAddress")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(false)
                        .withRead(false)
                        .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode(LINE1)
                            .withRole(ROLE_IN_USER_ROLES)
                            .withCreate(false)
                            .withUpdate(false)
                            .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode(LINE2)
                            .withRole(ROLE_IN_USER_ROLES)
                            .withCreate(true)
                            .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(
                    aViewField()
                        .withFieldType(aFieldType()
                            .withType(COMPLEX)
                            .withComplexField(
                                newCaseField()
                                    .withFieldType(
                                        aFieldType()
                                            .withType(TEXT)
                                            .withId(TEXT)
                                            .build())
                                    .withId(LINE1)
                                    .build())
                            .withComplexField(
                                newCaseField()
                                    .withFieldType(
                                        aFieldType()
                                            .withType(TEXT)
                                            .withId(TEXT)
                                            .build())
                                    .withId(LINE2)
                                    .build())
                            .build())
                        .withId("ResidenceAddress")
                        .build())
                .withField(
                    aViewField()
                        .withFieldType(aFieldType()
                            .withType(COMPLEX)
                            .withComplexField(
                                newCaseField()
                                    .withFieldType(
                                        aFieldType()
                                            .withType(TEXT)
                                            .withId(TEXT)
                                            .build())
                                    .withId(LINE1)
                                    .build())
                            .withComplexField(
                                newCaseField()
                                    .withFieldType(
                                        aFieldType()
                                            .withType(TEXT)
                                            .withId(TEXT)
                                            .build())
                                    .withId(LINE2)
                                    .build())
                            .build())
                        .withId("OfficeAddress")
                        .build())
                .build();

            assertEquals(2, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(0, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should not set readonly flag for complex children if relevant acl is there")
        void shouldNotSetReadonlyFlagForComplexChildrenIfRelevantAclIsThere() {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType()
                        .withType(COMPLEX)
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE1)
                                .build())
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE2)
                                .build())
                        .build())
                    .withId(ADDRESSES)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(true)
                        .withRead(true)
                        .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode(LINE1)
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(true)
                            .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode(LINE2)
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(true)
                            .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            CaseUpdateViewEvent caseEventTrigger = createCaseUpdateViewEventWithTwoSubFields(TEXT);

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields().get(0).getDisplayContext(), not(READONLY)),
                () -> assertThat(eventTrigger.getCaseFields().get(0).getComplexFieldNestedField(LINE1),
                    not(hasProperty("displayContext", is(READONLY)))),
                () -> assertThat(eventTrigger.getCaseFields().get(0).getComplexFieldNestedField(LINE2),
                    not(hasProperty("displayContext", is(READONLY))))
            );
        }

        @Test
        @DisplayName("Should set readonly flag for complex children and complex field overrides if relevant acl is "
            + "missing")
        void shouldSetReadonlyFlagForComplexChildrenIfRelevantAclIsMissing() {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType()
                        .withType(COMPLEX)
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE1)
                                .build())
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE2)
                                .build())
                        .build())
                    .withId(ADDRESSES)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(true)
                        .withRead(true)
                        .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode(LINE1)
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(true)
                            .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode(LINE2)
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(false)
                            .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            final CaseViewField caseViewField1 = aViewField()
                .withFieldType(aFieldType()
                    .withType(COMPLEX)
                    .withComplexField(
                        newCaseField()
                            .withFieldType(
                                aFieldType()
                                    .withType(TEXT)
                                    .withId(TEXT)
                                    .build())
                            .withId(LINE1)
                            .build())
                    .withComplexField(
                        newCaseField()
                            .withFieldType(
                                aFieldType()
                                    .withType(TEXT)
                                    .withId(TEXT)
                                    .build())
                            .withId(LINE2)
                            .build())
                    .build())
                .withId(ADDRESSES)
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
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields().get(0).getDisplayContext(), not(READONLY)),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField(LINE1).orElseThrow(() ->
                        new RuntimeException("Line 2 is not there")),
                    not(hasProperty("displayContext", is(READONLY)))
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField(LINE2).orElseThrow(() ->
                        new RuntimeException("Line 2 is not there")),
                    hasProperty("displayContext", is(READONLY))
                ),
                () -> assertThat(
                    eventTrigger.getWizardPages().get(0).getWizardPageFields().get(0).getComplexFieldOverrides().get(0)
                        .getDisplayContext(),
                    is(OPTIONAL)
                ),
                () -> assertThat(
                    eventTrigger.getWizardPages().get(0).getWizardPageFields().get(0).getComplexFieldOverrides().get(1)
                        .getDisplayContext(),
                    is(READONLY)
                )
            );
        }

        @Test
        @DisplayName("Should set readonly flag for collection children if relevant acl missing")
        void shouldSetReadonlyFlagForCollectionChildrenIfRelevantAclMissing() {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("AddressCollection")
                    .withFieldType(aFieldType()
                        .withType(COLLECTION)
                        .withCollectionField(newCaseField()
                            .withId(ADDRESSES)
                            .withFieldType(aFieldType()
                                .withType(COMPLEX)
                                .withComplexField(newCaseField()
                                    .withId(LINE1)
                                    .withFieldType(aFieldType()
                                        .withId(TEXT)
                                        .withType(TEXT)
                                        .build())
                                    .build())
                                .withComplexField(newCaseField()
                                    .withId(LINE2)
                                    .withFieldType(aFieldType()
                                        .withId(TEXT)
                                        .withType(TEXT)
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
                            .withListElementCode(ADDRESSES)
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
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            CaseUpdateViewEvent caseEventTrigger = createCaseUpdateViewEventForCollection();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields().get(0), not(hasProperty("displayContext",
                    is(READONLY)))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), ADDRESSES),
                    not(hasProperty("displayContext", is(READONLY)))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), "Addresses.Line1"),
                    hasProperty("displayContext", is(READONLY))),
                () -> assertThat(findNestedField(eventTrigger.getCaseFields().get(0), "Addresses.Line2"),
                    hasProperty("displayContext", is(READONLY)))
            );
        }

        @Test
        @DisplayName("Should set readonly flag for collection children if relevant acl missing with multiparty fix")
        void shouldSetReadonlyFlagForCollectionChildrenIfRelevantAclMissingWithMultiPartyFix() {
            setApplicationParamsForTest();

            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("AddressCollection")
                    .withFieldType(aFieldType()
                        .withType(COLLECTION)
                        .withCollectionField(newCaseField()
                            .withId(ADDRESSES)
                            .withFieldType(aFieldType()
                                .withType(COMPLEX)
                                .withComplexField(newCaseField()
                                    .withId(LINE1)
                                    .withFieldType(aFieldType()
                                        .withId(TEXT)
                                        .withType(TEXT)
                                        .build())
                                    .build())
                                .withComplexField(newCaseField()
                                    .withId(LINE2)
                                    .withFieldType(aFieldType()
                                        .withId(TEXT)
                                        .withType(TEXT)
                                        .build())
                                    .build())
                                .build())
                            .build())
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(false)
                        .build())
                    .withComplexACL(
                        aComplexACL()
                            .withListElementCode(ADDRESSES)
                            .withRole(ROLE_IN_USER_ROLES)
                            .withUpdate(false)
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

            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            CaseUpdateViewEvent caseEventTrigger = createCaseUpdateViewEventForCollection();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(0, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should not set readonly flag for collection children if relevant acl is there")
        void shouldNotSetReadonlyFlagForCollectionChildrenIfRelevantAclIsThere() {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("AddressCollection")
                    .withFieldType(aFieldType()
                        .withType(COLLECTION)
                        .withCollectionField(newCaseField()
                            .withId(ADDRESSES)
                            .withFieldType(aFieldType()
                                .withType(COMPLEX)
                                .withComplexField(newCaseField()
                                    .withId(LINE1)
                                    .withFieldType(aFieldType()
                                        .withId(TEXT)
                                        .withType(TEXT)
                                        .build())
                                    .build())
                                .withComplexField(newCaseField()
                                    .withId(LINE2)
                                    .withFieldType(aFieldType()
                                        .withId(TEXT)
                                        .withType(TEXT)
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
                            .withListElementCode(ADDRESSES)
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
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            CaseUpdateViewEvent caseEventTrigger = createCaseUpdateViewEventForCollection();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields().get(0), not(hasProperty("displayContext",
                    is(READONLY)))),
                () -> assertThat(eventTrigger.getCaseFields().get(0).getComplexFieldNestedField(ADDRESSES),
                    not(hasProperty("displayContext", is(READONLY)))),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField("Addresses.Line1"),
                    not(hasProperty("displayContext",
                        is(READONLY)))
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(0).getComplexFieldNestedField("Addresses.Line2"),
                    not(hasProperty("displayContext",
                        is(READONLY)))
                )
            );
        }

        @Test
        @DisplayName("Should set readonly flag if relevant acl not granting access")
        void shouldSetReadonlyFlagIfRelevantAclNotGrantingAccess() {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withId(ADDRESSES)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext",
                is(READONLY))));
        }

        @Test
        @DisplayName("Should set readonly flag if relevant acl not granting access with multiparty fix")
        void shouldSetReadonlyFlagIfRelevantAclNotGrantingAccessWithMultiPartyFix() {
            setApplicationParamsForTest();

            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withId(ADDRESSES)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .build())
                    .build())
                .build();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(0, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should set readonly flag if ACL true and event name not matching")
        void shouldSetReadonlyFlagIfRelevantAclGrantingAccessAndEventNameNotMatching() {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext",
                is(READONLY))));
        }

        @Test
        @DisplayName("Should set readonly flag if ACL true and event name not matching with multiparty fix")
        void shouldSetReadonlyFlagIfRelevantAclGrantingAccessAndEventNameNotMatchingWithMultiPartyFix() {
            setApplicationParamsForTest();

            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId(ADDRESSES)
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

            assertEquals(1, caseEventTrigger.getCaseFields().size());

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertEquals(0, eventTrigger.getCaseFields().size());
        }

        @Test
        @DisplayName("Should not set readonly flag if acl matching")
        void shouldNotSetReadonlyFlagIfAclMatching() {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withId(ADDRESSES)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(not(hasProperty("displayContext",
                is(READONLY)))));
        }

        @Test
        @DisplayName("Should not set readonly flag if acl matching in acls group")
        void shouldNotSetReadonlyFlagIfAclMatchingInAclsGroup() {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withId(ADDRESSES)
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
            CaseUpdateViewEvent caseEventTrigger = defaultCaseUpdateViewEvent();

            CaseUpdateViewEvent eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(not(hasProperty("displayContext",
                is(READONLY)))));

        }

        @Test
        @DisplayName("Should not set readonly flags if acls matching in fields group")
        void shouldNotSetReadonlyFlagsIfAclsMatchingInCaseViewFieldsGroup() {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withId(ADDRESSES)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withId("AddressesNoAccess")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_3)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withId("AddressesNoAccess2")
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType(TEXT).build())
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
                        .withFieldType(aFieldType().withType(TEXT).build())
                        .withId(ADDRESSES)
                        .build())
                .withField(
                    aViewField()
                        .withFieldType(aFieldType().withType(TEXT).build())
                        .withId("AddressesNoAccess")
                        .build())
                .withField(
                    aViewField()
                        .withFieldType(aFieldType().withType(TEXT).build())
                        .withId("AddressesNoAccess2")
                        .build())
                .withField(
                    aViewField()
                        .withFieldType(aFieldType().withType(TEXT).build())
                        .withId("Addresses2")
                        .build())
                .build();

            CaseUpdateViewEvent actual = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                CASE_TYPE_ID,
                CASE_REFERENCE,
                EVENT_ID,
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES,
                CAN_UPDATE);
            assertAll(
                () -> assertThat(actual.getCaseFields(), hasSize(4)),
                () -> assertThat(actual.getCaseFields(), hasItem(allOf(hasProperty("id", is(ADDRESSES)),
                    not(hasProperty("displayContext", is(READONLY)))))),
                () -> assertThat(actual.getCaseFields(), hasItem(allOf(hasProperty("id",
                    is("AddressesNoAccess")),
                    hasProperty("displayContext", is(READONLY))))),
                () -> assertThat(actual.getCaseFields(), hasItem(allOf(hasProperty("id",
                    is("AddressesNoAccess2")),
                    hasProperty("displayContext", is(READONLY))))),
                () -> assertThat(actual.getCaseFields(), hasItem(allOf(hasProperty("id", is("Addresses2")),
                    not(hasProperty("displayContext", is(READONLY))))))
            );
        }

        private CaseTypeDefinition defaultCaseTypeDefinition() {
            return newCaseType()
                .withField(newCaseField()
                    .withFieldType(aFieldType().withType(TEXT).build())
                    .withId(ADDRESSES)
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(true)
                        .withRead(true)
                        .build())
                    .build())
                .build();
        }

        private CaseUpdateViewEvent defaultCaseUpdateViewEvent() {
            return newCaseUpdateViewEvent()
                .withField(
                    aViewField()
                        .withFieldType(aFieldType().withType(TEXT).build())
                        .withId(ADDRESSES)
                        .build())
                .build();
        }
    }

    @Nested
    @DisplayName("case event definitions ACL tests")
    class ReturnsCaseEventsDataWithCaseEventAccessAclTests {

        @Test
        @DisplayName("Should not return case event definition for user with missing role")
        void shouldNotReturnCaseEventDefinitionForUserWithMissingRole() {
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

            assertThat(accessControlService.filterCaseEventsByAccess(
                caseType, ACCESS_PROFILES,
                CAN_CREATE),
                is(emptyCollectionOf(CaseEventDefinition.class)));
        }

        @Test
        @DisplayName("Should not return case event definition if relevant acl not granting access")
        void shouldNotReturnCaseEventDefinitionIfRelevantAclNotGrantingAccess() {
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

            assertThat(accessControlService.filterCaseEventsByAccess(
                caseType, ACCESS_PROFILES,
                CAN_CREATE),
                is(emptyCollectionOf(CaseEventDefinition.class)));
        }

        @Test
        @DisplayName("Should return case event definition if acl matching")
        void shouldReturnCaseEventDefinitionWithAclMatching() {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            List<CaseEventDefinition> result = accessControlService.filterCaseEventsByAccess(
                caseType, ACCESS_PROFILES,
                CAN_CREATE);

            assertAll(
                () -> assertThat(result, hasSize(1)),
                () -> assertThat(result, hasItem(hasProperty("id", is(EVENT_ID))))
            );
        }

        @Test
        @DisplayName("Should return single case event definition if acl matching from a group")
        void shouldReturnCaseEventDefinitionWithAclMatchingFromGroup() {
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

            List<CaseEventDefinition> result = accessControlService.filterCaseEventsByAccess(
                caseType, ACCESS_PROFILES,
                CAN_CREATE);
            assertAll(
                () -> assertThat(result, hasSize(1)),
                () -> assertThat(result, hasItem(hasProperty("id", is(EVENT_ID))))
            );
        }

        @Test
        @DisplayName("Should return case event definition if acls matching")
        void shouldReturnCaseEventDefinitionWithAclsMatching() {
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

            List<CaseEventDefinition> result = accessControlService.filterCaseEventsByAccess(
                caseType, ACCESS_PROFILES,
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
    class CaseViewFieldsAclTests {

        @Test
        @DisplayName("Should not return case event definition for user with missing role")
        void shouldNotReturnCaseEventDefinitionForUserWithMissingRole() {
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

            assertThat(accessControlService.filterCaseEventsByAccess(
                caseType, ACCESS_PROFILES,
                CAN_CREATE),
                is(emptyCollectionOf(CaseEventDefinition.class)));
        }

        @Test
        @DisplayName("Should not return case event definition if relevant acl not granting access")
        void shouldNotReturnCaseEventDefinitionIfRelevantAclNotGrantingAccess() {
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

            assertThat(accessControlService.filterCaseEventsByAccess(
                caseType, ACCESS_PROFILES,
                CAN_CREATE),
                is(emptyCollectionOf(CaseEventDefinition.class)));
        }

        @Test
        @DisplayName("Should return case event definition if acl matching")
        void shouldReturnCaseEventDefinitionWithAclMatching() {
            final CaseTypeDefinition caseType = newCaseType()
                .withEvent(newCaseEvent()
                    .withId(EVENT_ID)
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            List<CaseEventDefinition> result = accessControlService.filterCaseEventsByAccess(
                caseType, ACCESS_PROFILES,
                CAN_CREATE);

            assertAll(
                () -> assertThat(result, hasSize(1)),
                () -> assertThat(result, hasItem(hasProperty("id", is(EVENT_ID))))
            );
        }

        @Test
        @DisplayName("Should return single case event definition if acl matching from a group")
        void shouldReturnCaseEventDefinitionWithAclMatchingFromGroup() {
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

            List<CaseEventDefinition> result = accessControlService.filterCaseEventsByAccess(
                caseType, ACCESS_PROFILES,
                CAN_CREATE);
            assertAll(
                () -> assertThat(result, hasSize(1)),
                () -> assertThat(result, hasItem(hasProperty("id", is(EVENT_ID))))
            );
        }

        @Test
        @DisplayName("Should return case event definition if acls matching")
        void shouldReturnCaseEventDefinitionWithAclsMatching() {
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

            List<CaseEventDefinition> result = accessControlService.filterCaseEventsByAccess(
                caseType, ACCESS_PROFILES,
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
    class CRUDonCollectionTests {
        private JsonNode existingDataNode;
        private final String comma = ",";
        private final String collStart = "{  \"Addresses\":[  \n";
        private final String child1 = "         {  \n"
            + "            \"value\":{  \n"
            + "               \"Address\":\"address1\",\n"
            + "               \"Notes\": {\n"
            + "                   \"Note1\": \"someNote11\",\n"
            + "                   \"Note2\": \"someNote21\"\n"
            + "                }"
            + "            },\n"
            + "            \"id\":\"" + FIRST_CHILD_ID + "\"\n"
            + "         }\n";
        private final String child1Updated = "         {  \n"
            + "            \"value\":{  \n"
            + "               \"Address\":\"address1\",\n"
            + "               \"Notes\": {\n"
            + "                   \"Note1\": \"someNote11 Updated\",\n"
            + "                   \"Note2\": \"someNote21 Updated\"\n"
            + "                }"
            + "            },\n"
            + "            \"id\":\"" + FIRST_CHILD_ID + "\"\n"
            + "         }\n";
        private final String child2 = "         {  \n"
            + "            \"value\":{  \n"
            + "               \"Address\":\"address1\",\n"
            + "               \"Notes\": {\n"
            + "                   \"Note1\": \"someNote21\",\n"
            + "                   \"Note2\": \"someNote22\"\n"
            + "                }"
            + "            },\n"
            + "            \"id\":\"" + SECOND_CHILD_ID + "\"\n"
            + "         }\n";
        private final String newChild = "         {  \n"
            + "            \"value\":{  \n"
            + "               \"Address\":\"address3\",\n"
            + "               \"Notes\": {\n"
            + "                   \"Note1\": \"someNote31\",\n"
            + "                   \"Note2\": \"someNote32\"\n"
            + "                }"
            + "            },\n"
            + "            \"id\":\"null\"\n"
            + "         }\n";
        private final String newChildWithNoIdTag = "         {  \n"
            + "            \"value\":{  \n"
            + "               \"Address\":\"address3\",\n"
            + "               \"Notes\": {\n"
            + "                   \"Note1\": \"someNote31\",\n"
            + "                   \"Note2\": \"someNote32\"\n"
            + "                }"
            + "            }\n"
            + "         }\n";

        private final String collEnd = "      ]\n }\n";

        private CaseFieldDefinition addressField;
        private CaseTypeDefinition caseType;

        @BeforeEach
        void setUp() throws IOException {
            existingDataNode = getJsonNode(collStart + child1 + comma + child2 + collEnd);

            addressField = newCaseField()
                .withId(ADDRESSES)
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
                        .withId(TEXT)
                        .withType(TEXT)
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
                            .withId(TEXT)
                            .withType(TEXT)
                            .build())
                        .build())
                    .withComplexField(newCaseField()
                        .withId("Note2")
                        .withFieldType(aFieldType()
                            .withId(TEXT)
                            .withType(TEXT)
                            .build())
                        .build())
                    .build())
                .build();
        }

        @Test
        @DisplayName("Should fail if the caseField not found")
        void shouldFailIfCaseFieldDoesNotExist() throws IOException {
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + comma + child2 + comma + newChild + collEnd),
                    existingDataNode,
                    Collections.emptyList(),
                    ACCESS_PROFILES),
                is(false));
        }

        @Test
        @DisplayName("Should allow creation of new items on collection")
        void shouldGrantCreateAccessToCollectionType() throws IOException {
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + comma + child2 + comma + newChild + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    ACCESS_PROFILES),
                is(true));
        }

        @Test
        @DisplayName("Should allow creation of new items on collection even when no Id provided")
        void shouldGrantCreateAccessToCollectionTypeWOutId() throws IOException {
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + comma + child2 + comma + newChildWithNoIdTag + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    ACCESS_PROFILES),
                is(true));
        }

        @Test
        @DisplayName("Should not allow creation of new items on collection")
        void shouldNotGrantCreateAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(
                singletonList(anAcl().withRole(ROLE_IN_USER_ROLES).withCreate(false).build()));
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + comma + child2 + comma + newChild + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    ACCESS_PROFILES),
                is(false));
        }


        @Test
        @DisplayName("Should allow update of items on collection")
        void shouldGrantUpdateAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(
                singletonList(anAcl().withRole(ROLE_IN_USER_ROLES).withUpdate(true).build()));
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1Updated + comma + child2 + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    ACCESS_PROFILES),
                is(true));
        }

        @Test
        @DisplayName("Should allow update of items on collection along with creation")
        void shouldGrantUpdateAndCreateAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(
                singletonList(anAcl().withRole(ROLE_IN_USER_ROLES).withCreate(true)
                    .withUpdate(true).build()));
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1Updated + comma + child2 + comma + newChildWithNoIdTag
                        + comma + newChild + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    ACCESS_PROFILES),
                is(true));
        }

        @Test
        @DisplayName("Should not allow update of items on collection")
        void shouldNotGrantUpdateAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(
                singletonList(anAcl().withRole(ROLE_IN_USER_ROLES).withUpdate(false).build()));
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1Updated + comma + child2 + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    ACCESS_PROFILES),
                is(false));
        }

        @Test
        @DisplayName("Should allow deletion of items on collection")
        void shouldGrantDeleteAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(
                singletonList(anAcl().withRole(ROLE_IN_USER_ROLES).withDelete(true).build()));
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    ACCESS_PROFILES),
                is(true));
        }

        @Test
        @DisplayName("Should allow deletion of items on collection along with creation")
        void shouldGrantDeleteAndCreateAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(
                singletonList(anAcl().withRole(ROLE_IN_USER_ROLES).withCreate(true)
                    .withDelete(true).build()));
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + comma + newChildWithNoIdTag + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    ACCESS_PROFILES),
                is(true));
        }

        @Test
        @DisplayName("Should not allow deletion of items on collection")
        void shouldNotGrantDeleteAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(
                singletonList(anAcl().withRole(ROLE_IN_USER_ROLES).withDelete(false).build()));
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1 + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    ACCESS_PROFILES),
                is(false));
        }


        @Test
        @DisplayName("Should allow creation, updating and deletion of items on collection")
        void shouldGrantUpdateDeleteAndCreateAccessToCollectionType() throws IOException {
            addressField.setAccessControlLists(
                singletonList(anAcl().withRole(ROLE_IN_USER_ROLES).withCreate(true)
                    .withUpdate(true).withDelete(true).build()));
            caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

            assertThat(
                accessControlService.canAccessCaseFieldsForUpsert(
                    getJsonNode(collStart + child1Updated + comma + child2 + comma
                        + newChildWithNoIdTag + collEnd),
                    existingDataNode,
                    caseType.getCaseFieldDefinitions(),
                    ACCESS_PROFILES),
                is(true));
        }
    }

    @Nested
    @DisplayName("Case View Field with Criteria tests")
    class CanAccessCaseViewFieldWithCriteriaTests {

        @Test
        @DisplayName("Should allow access when the role has read permission")
        void shouldGrantAccessWhenRoleHasReadPermissionForField() {
            final CommonField viewField =
                newCaseField()
                    .withId("NotesReadAccessForRole")
                    .withFieldType(aFieldType()
                        .withType(TEXT)
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withRead(true)
                        .build())
                    .build();

            setupLogging().setLevel(Level.DEBUG);
            boolean canAccessCaseViewField =
                accessControlService.canAccessCaseViewFieldWithCriteria(viewField, ACCESS_PROFILES, CAN_READ);

            loggingEventList = listAppender.list;

            assertAll(
                () -> assertThat(canAccessCaseViewField, is(true)),
                () -> assertThat(loggingEventList.size(), is(0))
            );
        }

        @Test
        @DisplayName("Should not grant access to case view for user with missing role")
        void shouldNotGrantAccessToCaseViewForUserWithMissingRole() {
            final CommonField viewField = newCaseField()
                    .withId("NotesNoReadAccessForRole")
                    .withFieldType(aFieldType()
                            .withType(TEXT)
                            .build())
                    .withAcl(anAcl()
                            .withRole(ROLE_NOT_IN_USER_ROLES)
                            .withRead(true)
                            .build())
                    .build();

            setupLogging().setLevel(Level.DEBUG);
            assertFalse(accessControlService.canAccessCaseViewFieldWithCriteria(viewField, ACCESS_PROFILES, CAN_READ));

            loggingEventList = listAppender.list;
            String expectedLogMessage = TestBuildersUtil.formatLogMessage(
                    NO_ROLE_FOUND, "caseField", "NotesNoReadAccessForRole",
                    extractAccessProfileNames(ACCESS_PROFILES), "caseFieldACL",
                    "[ACL{accessProfile='caseworker-divorce-loa4', crud=R}]");

            assertAll(
                () -> assertThat(loggingEventList.get(0).getLevel(), is(Level.DEBUG)),
                () -> assertThat(loggingEventList.get(0).getFormattedMessage(), is(expectedLogMessage))
            );
        }
    }

    private JsonNode getJsonNode(String content) throws IOException {
        final Map<String, JsonNode> newData = JacksonUtils.convertValue(MAPPER.readTree(content));
        return JacksonUtils.convertValueJsonNode(newData);
    }

    private void assertFieldsAccess(boolean hasFieldAccess, CaseTypeDefinition caseType, JsonNode newDataNode,
                                    JsonNode existingDataNode) {
        assertThat(
            accessControlService.canAccessCaseFieldsForUpsert(
                newDataNode,
                existingDataNode,
                caseType.getCaseFieldDefinitions(),
                ACCESS_PROFILES),
            is(hasFieldAccess));
    }

    private Logger setupLogging() {
        listAppender = new ListAppender<>();
        listAppender.start();
        logger = (Logger) LoggerFactory.getLogger(
                        logServiceClass ? AccessControlService.class : AccessControlServiceImpl.class);
        logger.detachAndStopAllAppenders();
        if (loggingEventList != null && !loggingEventList.isEmpty()) {
            loggingEventList.clear();
        }
        logger.addAppender(listAppender);
        logServiceClass = false;
        return logger;
    }

    @After
    public void tearDown() {
        if (listAppender != null) {
            listAppender.stop();
        }
        if (logger != null) {
            logger.detachAndStopAllAppenders();
        }
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
                    .withId(TEXT)
                    .withType(TEXT)
                    .build())
                .build())
            .withComplexField(newCaseField()
                .withId("LastName")
                .withFieldType(aFieldType()
                    .withId(TEXT)
                    .withType(TEXT)
                    .build())
                .build())
            .withComplexField(getBirtInfoComplexField())
            .withComplexField(getAddressesCollectionFieldDefinition())
            .withComplexField(getNotesCollectionFieldDefinition())
            .build();
    }

    static CaseFieldDefinition getAddressesCollectionFieldDefinition() {
        CaseFieldDefinition caseField = newCaseField()
            .withId(ADDRESSES)
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
                        .withId(TEXT)
                        .withType(TEXT)
                        .build())
                    .build())
                .withComplexField(newCaseField()
                    .withId("BornCountry")
                    .withFieldType(aFieldType()
                        .withId(TEXT)
                        .withType(TEXT)
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
                    .withId(TEXT)
                    .withType(TEXT)
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
                .withId(LINE1)
                .withFieldType(aFieldType()
                    .withId(TEXT)
                    .withType(TEXT)
                    .build())
                .build())
            .withComplexField(newCaseField()
                .withId(LINE2)
                .withFieldType(aFieldType()
                    .withId(TEXT)
                    .withType(TEXT)
                    .build())
                .build())
            .withComplexField(newCaseField()
                .withId("PostCode")
                .withFieldType(aFieldType()
                    .withId(TEXT)
                    .withType(TEXT)
                    .build())
                .build())
            .withComplexField(newCaseField()
                .withId("Country")
                .withFieldType(aFieldType()
                    .withId(TEXT)
                    .withType(TEXT)
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
                    .withId(TEXT)
                    .withType(TEXT)
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
                    .withId(TEXT)
                    .withType(TEXT)
                    .build())
                .build())
            .withComplexField(newCaseField()
                .withId("Category")
                .withFieldType(aFieldType()
                    .withId(TEXT)
                    .withType(TEXT)
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

    private static Set<AccessProfile> createAccessProfiles(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> AccessProfile.builder().readOnly(false)
                .accessProfile(userRole)
                .build())
            .collect(Collectors.toSet());
    }

    private void setApplicationParamsForTest() {
        Mockito.when(applicationParams.isMultipartyFixEnabled()).thenReturn(true);
        Mockito.when(applicationParams.getMultipartyCaseTypes()).thenReturn(singletonList(CASE_TYPE_ID));
        Mockito.when(applicationParams.getMultipartyEvents()).thenReturn(singletonList(EVENT_ID));
    }

    private CaseTypeDefinition createCaseTypeWithTwoSubFields(String type) {
        final CaseTypeDefinition caseType = newCaseType()
            .withField(newCaseField()
                .withFieldType(aFieldType()
                    .withType(COMPLEX)
                    .withComplexField(
                        newCaseField()
                            .withFieldType(
                                aFieldType()
                                    .withType(TEXT)
                                    .withId(TEXT)
                                    .build())
                            .withId(LINE1)
                            .build())
                    .withComplexField(
                        newCaseField()
                            .withFieldType(
                                aFieldType()
                                    .withType(type)
                                    .withId(TEXT)
                                    .build())
                            .withId(LINE2)
                            .build())
                    .build())
                .withId(ADDRESSES)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .withUpdate(false)
                    .withRead(true)
                    .build())
                .withComplexACL(
                    aComplexACL()
                        .withListElementCode(LINE1)
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(false)
                        .withUpdate(false)
                        .withRead(true)
                        .build())
                .withComplexACL(
                    aComplexACL()
                        .withListElementCode(LINE2)
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withRead(true)
                        .build())
                .build())
            .build();

        caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);
        return caseType;
    }

    private CaseTypeDefinition createCaseTypeWithThreeSubFields(String type) {
        final CaseTypeDefinition caseType = newCaseType()
            .withField(newCaseField()
                .withFieldType(aFieldType()
                    .withType(COMPLEX)
                    .withComplexField(
                        newCaseField()
                            .withFieldType(
                                aFieldType()
                                    .withType(TEXT)
                                    .withId(TEXT)
                                    .build())
                            .withId(LINE1)
                            .build())
                    .withComplexField(
                        newCaseField()
                            .withFieldType(
                                aFieldType()
                                    .withType(TEXT)
                                    .withId(TEXT)
                                    .build())
                            .withId(LINE2)
                            .build())
                    .withComplexField(
                        newCaseField()
                            .withFieldType(
                                aFieldType()
                                    .withType(type)
                                    .withId(TEXT)
                                    .build())
                            .withId(LINE3)
                            .build())
                    .build())
                .withId(ADDRESSES)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .withUpdate(true)
                    .withRead(true)
                    .build())
                .withComplexACL(
                    aComplexACL()
                        .withListElementCode(LINE1)
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(true)
                        .withRead(true)
                        .build())
                .withComplexACL(
                    aComplexACL()
                        .withListElementCode(LINE2)
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .withUpdate(false)
                        .withRead(false)
                        .build())
                .build())
            .build();

        caseType.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);
        return caseType;
    }

    private CaseUpdateViewEvent createCaseUpdateViewEventWithTwoSubFields(String type) {
        return newCaseUpdateViewEvent()
            .withField(
                aViewField()
                    .withFieldType(aFieldType()
                        .withType(COMPLEX)
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE1)
                                .build())
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(type)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE2)
                                .build())
                        .build())
                    .withId(ADDRESSES)
                    .build())
            .build();
    }

    private CaseUpdateViewEvent createCaseUpdateViewEventWithThreeSubFields(String type) {
        return newCaseUpdateViewEvent()
            .withField(
                aViewField()
                    .withFieldType(aFieldType()
                        .withType(COMPLEX)
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE1)
                                .build())
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(TEXT)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE2)
                                .build())
                        .withComplexField(
                            newCaseField()
                                .withFieldType(
                                    aFieldType()
                                        .withType(type)
                                        .withId(TEXT)
                                        .build())
                                .withId(LINE3)
                                .build())
                        .build())
                    .withId(ADDRESSES)
                    .build())
            .build();
    }

    private CaseUpdateViewEvent createCaseUpdateViewEventForCollection() {
        return newCaseUpdateViewEvent()
            .withField(aViewField()
                .withId("AddressCollection")
                .withFieldType(aFieldType()
                    .withType(COLLECTION)
                    .withCollectionField(newCaseField()
                        .withId(ADDRESSES)
                        .withFieldType(aFieldType()
                            .withType(COMPLEX)
                            .withComplexField(newCaseField()
                                .withId(LINE1)
                                .withFieldType(aFieldType()
                                    .withId(TEXT)
                                    .withType(TEXT)
                                    .build())
                                .build())
                            .withComplexField(
                                newCaseField()
                                    .withId(LINE2)
                                    .withFieldType(aFieldType()
                                        .withId(TEXT)
                                        .withType(TEXT)
                                        .build())
                                    .build())
                            .build())
                        .build())
                    .build())
                .build())
            .build();
    }
}
