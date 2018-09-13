package uk.gov.hmcts.ccd.domain.service.common;

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
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AuditEventBuilder.anAuditEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.anCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventTriggerBuilder.anEventTrigger;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.aCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseStateBuilder.newState;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewFieldBuilder.aViewField;

public class AccessControlServiceTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String EVENT_ID_WITH_ACCESS = "EVENT_ID_WITH_ACCESS";
    private static final String EVENT_ID_WITHOUT_ACCESS = "EVENT_ID_WITHOUT_ACCESS";
    private static final String EVENT_ID_WITHOUT_ACCESS_2 = "EVENT_ID_WITHOUT_ACCESS_2";
    private static final String EVENT_ID_WITH_ACCESS_2 = "EVENT_ID_WITH_ACCESS_2";
    private final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        accessControlService = new AccessControlService();
    }
    @Nested
    @DisplayName("ACL tests - CaseState")
    class CanAccessCaseStateWithCriteria_AclTests {

        @Test
        @DisplayName("Should not grant access to case state with relevant acl missing")
        void shouldNotGrantAccessToStateIfRelevantACLMissing() {
            CaseType caseType = newCaseType()
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
            CaseType caseType = newCaseType()
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
            CaseType caseType = newCaseType()
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
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID1, caseType, USER_ROLES,CAN_CREATE),is(true)),
                () -> assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID2, caseType, USER_ROLES,CAN_CREATE),is(true))
            );
        }

        @Test
        @DisplayName("Shouldn't grant access to state when state is not present in definition")
        void shouldNotGrantAccessToStateIfStateIsNotPresentInDefinition() throws IOException {
            CaseType caseType = newCaseType().build();

            assertThat(accessControlService.canAccessCaseStateWithCriteria(STATE_ID1, caseType,USER_ROLES,CAN_CREATE), is(false));
        }

        @Test
        @DisplayName("Should filter states according to acls")
        void shouldFilterStatesAccordingToACLs() {
            CaseState caseState1 = newState()
                .withId(STATE_ID1)
                .withAcl(anAcl()
                             .withRole(ROLE_IN_USER_ROLES)
                             .withRead(true)
                             .build())
                .build();
            CaseState caseState2 = newState()
                .withId(STATE_ID2)
                .withAcl(anAcl()
                             .withRole(ROLE_IN_USER_ROLES)
                             .build())
                .build();
            List<CaseState> caseStates = new ArrayList<>(Arrays.asList(caseState1, caseState2));
            final List<CaseState> states = accessControlService.filterCaseStatesByAccess(caseStates, USER_ROLES, CAN_READ);

            assertAll(
                () -> assertThat(states.size(), is(1)),
                () -> assertThat(states, hasItem(caseState1)),
                () -> assertThat(states, not(hasItem(caseState2)))
            );
        }

        @Test
        @DisplayName("Should filter states out when no matching ACLs")
        void shouldFilterOutStatesWhenNoMatchingACLSs() {
            CaseState caseState1 = newState()
                .withId(STATE_ID1)
                .withAcl(anAcl()
                             .withRole(ROLE_IN_USER_ROLES)
                             .build())
                .build();
            CaseState caseState2 = newState()
                .withId(STATE_ID2)
                .withAcl(anAcl()
                             .withRole(ROLE_IN_USER_ROLES)
                             .build())
                .build();
            CaseState caseState3 = newState()
                .withId("Some State")
                .withAcl(anAcl()
                             .withRole(ROLE_IN_USER_ROLES)
                             .build())
                .build();
            List<CaseState> caseStates = new ArrayList<>(Arrays.asList(caseState1, caseState2, caseState3));
            final List<CaseState> states = accessControlService.filterCaseStatesByAccess(caseStates, USER_ROLES, CAN_READ);

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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"SomeText\" }\n"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFields(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to case fields with relevant acl missing")
        void shouldNotGrantAccessToFieldsIfRelevantAclMissing() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_NOT_IN_USER_ROLES)
                                            .withCreate(true)
                                            .withRead(true)
                                            .build())
                        .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFields(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to case fields with relevant acl not granting access")
        void shouldNotGrantAccessToFieldsIfRelevantAclNotGrantingAccess() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_2)
                                            .build())
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("Addresses2")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_2)
                                            .build())
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_2)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\", " +
                    "   \"Addresses2\": \"someText\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFields(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should not grant access to case fields if ACL false and null value")
        void shouldNotGrantAccessToNullValue() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_3)
                                            .build())
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": null }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFields(),
                    USER_ROLES,
                    CAN_CREATE),
                is(false));
        }

        @Test
        @DisplayName("Should grant access to case fields with acl matching")
        void shouldGrantAccessToFieldsWithAclMatching() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_3)
                                            .build())
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .build())
                .withField(aCaseField()
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
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\", " +
                    "   \"Addresses2\": \"someText\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFields(),
                    USER_ROLES,
                    CAN_CREATE),
                is(true));
        }

        @Test
        @DisplayName("Should grant access to case fields when field is no present in definition")
        void shouldGrantAccessToFieldsIfFieldNotPresentInDefinition() throws IOException {
            CaseType caseType = newCaseType()
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFields(),
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFields(),
                    USER_ROLES,
                    CAN_CREATE),
                is(true));
        }

        @Test
        @DisplayName("Should not grant access to case fields if ACL false and empty text")
        void shouldNotGrantAccessToEmptyTextType() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFields(),
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\":[  \n" +
                    "         {  \n" +
                    "            \"value\":{  \n" +
                    "               \"Address\":\"address1\",\n" +
                    "               \"Notes\": {\n" +
                    "                   \"Note1\": \"someNote11\",\n" +
                    "                   \"Note2\": \"someNote21\"\n" +
                    "                }" +
                    "            },\n" +
                    "            \"id\":\"" + FIRST_CHILD_ID + "\"\n" +
                    "         },\n" +
                    "         {  \n" +
                    "            \"value\":{  \n" +
                    "               \"Address\":\"address1\",\n" +
                    "               \"Notes\": {\n" +
                    "                   \"Note1\": \"someNote21\",\n" +
                    "                   \"Note2\": \"someNote22\"\n" +
                    "                }" +
                    "            },\n" +
                    "            \"id\":\"" + SECOND_CHILD_ID + "\"\n" +
                    "         }\n" +
                    "      ]\n" +
                    "    }\n"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFields(),
                    USER_ROLES,
                    CAN_CREATE),
                is(true));
        }

        @Test
        @DisplayName("Should not grant access to case fields if ACL false and empty collection")
        void shouldNotGrantCreateAccessToCollectionTypeIfEmpty() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\":[] }\n"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFields(),
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\":{  \n" +
                    "          \"Note\": \"someNote11\"\n" +
                    "       }\n" +
                    "    }\n"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFields(),
                    USER_ROLES,
                    CAN_CREATE),
                is(true));
        }

        @Test
        @DisplayName("Should not grant access to case fields if ACL false and empty object")
        void shouldNotGrantAccessToComplexTypeIfEmpty() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\":{} }\n"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            assertThat(
                accessControlService.canAccessCaseFieldsWithCriteria(
                    dataNode,
                    caseType.getCaseFields(),
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\" : \"UpdateAddress\" }");
            JsonNode existingDataNode = getJsonNode("{  \"Addresses\": \"SomeText\" }");

            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to field if field acls are missing for create")
        void shouldNotGrantAccessToFieldsIfFieldIsMissingAclsForCreate() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("FirstName")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withUpdate(true)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("LastName")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("Mobile")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withUpdate(true)
                                            .build())
                               .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\": \"CreateAddress\"," +
                                                   " \"FirstName\": \"John\"," +
                                                   " \"LastName\": \"Smith\" }");
            JsonNode existingDataNode = getJsonNode("{ \"FirstName\": \"Mark\"," +
                                                        " \"Mobile\": \"07234543543\"," +
                                                        " \"LastName\": \"Smith\" }");

            assertFieldsAccess(true, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to case fields if a field does not have access granted")
        void shouldNotGrantAccessToFieldsIfOneFieldDoesNotHaveAccessGranted() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("FirstName")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("LastName")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("Mobile")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\": \"CreateAddress\"," +
                                                   " \"FirstName\": \"John\"," +
                                                   " \"LastName\": \"Smith\" }");
            JsonNode existingDataNode = getJsonNode("{ \"FirstName\": \"Mark\"," +
                                                        " \"Mobile\": \"07234543543\"," +
                                                        " \"LastName\": \"Smith\" }");


            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }

        @Test
        @DisplayName("Should not grant access to case fields if a field does not have acls")
        void shouldNotGrantAccessToFieldsIfOneFieldDoesNotHaveAcls() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("FirstName")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("LastName")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .build();
            JsonNode newDataNode = getJsonNode("{ \"Addresses\": \"CreateAddress\"," +
                                                   " \"FirstName\": \"John\"," +
                                                   " \"LastName\": \"Smith\" }");
            JsonNode existingDataNode = getJsonNode("{ \"FirstName\": \"Mark\"," +
                                                        " \"Mobile\": \"07234543543\"," +
                                                        " \"LastName\": \"Smith\" }");


            assertFieldsAccess(false, caseType, newDataNode, existingDataNode);
        }
    }

    @Nested
    @DisplayName("case event ACL tests")
    class CanAccessCaseEventWithCriteria_AclTests {

        @Test
        @DisplayName("Should not grant access to event if acls are missing")
        void shouldNotGrantAccessToEventIfEventIsMissingAcls() throws IOException {
            final CaseType caseType = new CaseType();
            CaseEvent eventDefinition = new CaseEvent();
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
            final CaseType caseType = new CaseType();
            CaseEvent eventDefinition = new CaseEvent();
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = new CaseType();

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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"SomeText\" }\n"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should not return data if field with relevant acl missing")
        void shouldNotReturnFieldIfRelevantAclMissing() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_NOT_IN_USER_ROLES)
                                            .withRead(true)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should not return data if field with relevant acl not granting access")
        void shouldNotGrantAccessToFieldsIfRelevantAclNotGrantingAccess() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should not return data if field with acl false and null value")
        void shouldNotReturnDataWithAclFalseAndNullValue() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_3)
                                            .build())
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": null }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should not return data if field with acl true and field name not matching")
        void shouldNotReturnDataWithAclTrueAndFieldNameNotMatching() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"addresses\": \"someText\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should return data if field with acl true and null value")
        void shouldReturnDataWithAclTrueAndNullValue() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": null }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode, is(equalTo(dataNode)));
        }

        @Test
        @DisplayName("Should return data if field with acl true and empty value")
        void shouldReturnDataWithAclTrueAndEmptyValue() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode, is(equalTo(dataNode)));
        }

        @Test
        @DisplayName("Should return data if field with acl matching")
        void shouldGrantAccessToFieldsWithAclMatching() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"someText\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode, is(equalTo(dataNode)));
        }
    }

    @Nested
    @DisplayName("return fields data with text value tests")
    class ReturnsDataWithCaseFieldAccess_TextValueType {

        @Test
        @DisplayName("Should not return data if field ACL false and empty text")
        void shouldNotGrantAccessToEmptyTextType() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": \"\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }

        @Test
        @DisplayName("Should return data with null and empty values on root level")
        void shouldReturnDataWithNullAndEmptyValuesOnRootLevel() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withRead(true)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("Addresses2")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withRead(true)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": null,\n" +
                    "       \"Addresses2\": \"\" }"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withRead(true)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{\n" +
                    "         \"Addresses\":[  \n" +
                    "         {  \n" +
                    "            \"value\":{  \n" +
                    "               \"Address\":\"address1\",\n" +
                    "               \"Notes\": {\n" +
                    "                   \"Note1\": \"someNote11\",\n" +
                    "                   \"Note2\": \"someNote21\"\n" +
                    "                }" +
                    "            },\n" +
                    "            \"id\":\"" + FIRST_CHILD_ID + "\"\n" +
                    "         },\n" +
                    "         {  \n" +
                    "            \"value\":{  \n" +
                    "               \"Address\":\"address2\",\n" +
                    "               \"Notes\": {\n" +
                    "                   \"Note1\": \"someNote21\",\n" +
                    "                   \"Note2\": \"someNote22\"\n" +
                    "                }" +
                    "            },\n" +
                    "            \"id\":\"" + SECOND_CHILD_ID + "\"\n" +
                    "         }\n" +
                    "      ]\n" +
                    "    }\n"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode, is(equalTo(dataNode)));
        }


        @Test
        @DisplayName("Should return data with null and empty values on root level")
        void shouldReturnDataWithNullAndEmptyValuesOnRootLevel() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withRead(true)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("Addresses2")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withRead(true)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("Addresses3")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withRead(true)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{    \n" +
                    "         \"Addresses\":[  \n" +
                    "         {  \n" +
                    "            \"value\":{  \n" +
                    "               \"Address\":\"address1\",\n" +
                    "               \"Notes\": {\n" +
                    "                   \"Note1\": \"someNote11\",\n" +
                    "                   \"Note2\": \"someNote21\"\n" +
                    "                }" +
                    "            },\n" +
                    "            \"id\":\"" + FIRST_CHILD_ID + "\"\n" +
                    "         },\n" +
                    "         {  \n" +
                    "            \"value\":{  \n" +
                    "               \"Address\":\"address2\",\n" +
                    "               \"Notes\": {\n" +
                    "                   \"Note1\": \"someNote21\",\n" +
                    "                   \"Note2\": \"someNote22\"\n" +
                    "                }" +
                    "            },\n" +
                    "            \"id\":\"" + SECOND_CHILD_ID + "\"\n" +
                    "         }\n" +
                    "        ],\n" +
                    "      \"Addresses2\": [],\n" +
                    "      \"Addresses3\": null\n" +
                    "    }\n"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

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
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\":[] }\n"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }
    }

    @Nested
    @DisplayName("return fields data with complex value tests")
    class ReturnsDataWithCaseFieldReadAccess_ComplexValueType {

        @Test
        @DisplayName("Should return data if field with complex object")
        void shouldGrantAccessToComplexType() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withRead(true)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\":{  \n" +
                    "           \"Note\": \"someNote11\"\n" +
                    "       }\n" +
                    "    }\n"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode.get("Addresses").get("Note"), is(getTextNode("someNote11")));
        }

        @Test
        @DisplayName("Should return data with null and empty values on root level")
        void shouldReturnDataWithNullAndEmptyValuesOnRootLevel() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withRead(true)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("Addresses2")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withRead(true)
                                            .build())
                               .build())
                .build();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\": null,\n" +
                    "      \"Addresses2\":{  \n" +
                    "           \"Note\": \"\"\n" +
                    "       }\n" +
                    "    }\n"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertAll(
                () -> assertThat(jsonNode.get("Addresses"), is(JSON_NODE_FACTORY.nullNode())),
                () -> assertThat(jsonNode.get("Addresses2").get("Note"), is(getTextNode("")))
            );
        }

        @Test
        @DisplayName("Should not return data if field ACL false and empty object")
        void shouldNotGrantAccessToComplexTypeIfEmpty() throws IOException {
            CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .build();
            List<CaseField> caseFields = newArrayList();
            final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
                "{  \"Addresses\":{} }\n"
            ), STRING_JSON_MAP);
            JsonNode dataNode = MAPPER.convertValue(data, JsonNode.class);

            JsonNode jsonNode = accessControlService.filterCaseFieldsByAccess(
                dataNode,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_READ);

            assertThat(jsonNode, is(equalTo(JSON_NODE_FACTORY.objectNode())));
        }
    }

    @Nested
    @DisplayName("case type ACL tests")
    class ReturnsDataWithCaseType_AclTests {

        @Test
        @DisplayName("Should not return event if event is missing")
        void shouldNotReturnEventIfCaseEventIsMissing() throws IOException {
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent().withId(EVENT_ID_WITH_ACCESS)
                                    .withAcl(anAcl()
                                                 .withRole(ROLE_IN_USER_ROLES)
                                                 .withRead(true)
                                                 .build())
                                    .withAcl(anAcl()
                                                 .withRole(ROLE_NOT_IN_USER_ROLES)
                                                 .build())
                           .build())
                .withEvent(anCaseEvent()
                               .withId(EVENT_ID_WITHOUT_ACCESS)
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_3)
                                            .build())
                            .build())
                .withEvent(anCaseEvent()
                               .withId(EVENT_ID_WITHOUT_ACCESS_2)
                               .withAcl(anAcl()
                                            .withRole(ROLE_NOT_IN_USER_ROLES)
                                            .withRead(true)
                                            .build())
                           .build())
                .withEvent(anCaseEvent()
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
            final CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_NOT_IN_USER_ROLES)
                                            .withCreate(true)
                                            .withUpdate(true)
                                            .withRead(true)
                                            .build())
                               .build())
                .build();

            CaseEventTrigger caseEventTrigger = anEventTrigger()
                .withField(
                    aViewField()
                        .withId("Addresses")
                        .build())
                .build();

            CaseEventTrigger eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext", is("READONLY"))));
        }

        @Test
        @DisplayName("Should set readonly flag if relevant acl not granting access")
        void shouldSetReadonlyFlagIfRelevantAclNotGrantingAccess() throws IOException {
            final CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .build())
                               .build())
                .build();
            CaseEventTrigger caseEventTrigger = anEventTrigger()
                .withField(
                    aViewField()
                        .withId("Addresses")
                        .build())
                .build();

            CaseEventTrigger eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext", is("READONLY"))));
        }

        @Test
        @DisplayName("Should set readonly flag if ACL true and event name not matching")
        void shouldSetReadonlyFlagIfRelevantAclGrantingAccessAndEventNameNotMatching() throws IOException {
            final CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withUpdate(true)
                                            .build())
                               .build())
                .build();
            CaseEventTrigger caseEventTrigger = anEventTrigger()
                .withField(
                    aViewField()
                        .withId("DifferentAddresses")
                        .build())
                .build();

            CaseEventTrigger eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(hasProperty("displayContext", is("READONLY"))));
        }

        @Test
        @DisplayName("Should not set readonly flag if acl matching")
        void shouldNotSetReadonlyFlagIfAclMatching() throws IOException {
            final CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withUpdate(true)
                                            .build())
                               .build())
                .build();
            CaseEventTrigger caseEventTrigger = anEventTrigger()
                .withField(
                    aViewField()
                        .withId("Addresses")
                        .build())
                .build();

            CaseEventTrigger eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
                caseEventTrigger,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(not(hasProperty("displayContext", is("READONLY")))));
        }

        @Test
        @DisplayName("Should not set readonly flag if acl matching in acls group")
        void shouldNotSetReadonlyFlagIfAclMatchingInAclsGroup() throws IOException {
            final CaseType caseType = newCaseType()
                .withField(aCaseField()
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
            CaseEventTrigger caseEventTrigger = anEventTrigger()
                .withField(
                    aViewField()
                        .withId("Addresses")
                        .build())
                .build();

            CaseEventTrigger eventTrigger = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(caseEventTrigger,
                                                                                                       caseType.getCaseFields(),
                                                                                                       USER_ROLES,
                                                                                                       CAN_UPDATE);

            assertThat(eventTrigger.getCaseFields(), everyItem(not(hasProperty("displayContext", is("READONLY")))));

        }

        @Test
        @DisplayName("Should not set readonly flags if acls matching in fields group")
        void shouldNotSetReadonlyFlagsIfAclsMatchingInCaseViewFieldsGroup() throws IOException {
            final CaseType caseType = newCaseType()
                .withField(aCaseField()
                               .withId("Addresses")
                               .withAcl(anAcl()
                                            .withRole(ROLE_NOT_IN_USER_ROLES)
                                            .build())
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withUpdate(true)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("AddressesNoAccess")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_3)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("AddressesNoAccess2")
                               .withAcl(anAcl()
                                            .withRole(ROLE_NOT_IN_USER_ROLES)
                                            .build())
                               .build())
                .withField(aCaseField()
                               .withId("Addresses2")
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_2)
                                            .withUpdate(true)
                                            .build())
                               .build())
                .build();

            CaseEventTrigger caseEventTrigger = anEventTrigger()
                .withField(
                    aViewField()
                        .withId("Addresses")
                        .build())
                .withField(
                    aViewField()
                        .withId("AddressesNoAccess")
                        .build())
                .withField(
                    aViewField()
                        .withId("AddressesNoAccess2")
                        .build())
                .withField(
                    aViewField()
                        .withId("Addresses2")
                        .build())
                .build();

            CaseEventTrigger actual = accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(caseEventTrigger,
                                                                                                 caseType.getCaseFields(),
                                                                                                 USER_ROLES,
                                                                                                 CAN_UPDATE);
            assertAll(
                () -> assertThat(actual.getCaseFields(), hasSize(4)),
                () -> assertThat(actual.getCaseFields(), hasItem(allOf(hasProperty("id", is("Addresses")),
                                                                       not(hasProperty("displayContext", is("READONLY")))))),
                () -> assertThat(actual.getCaseFields(), hasItem(allOf(hasProperty("id", is("AddressesNoAccess")),
                                                                       hasProperty("displayContext", is("READONLY"))))),
                () -> assertThat(actual.getCaseFields(), hasItem(allOf(hasProperty("id", is("AddressesNoAccess2")),
                                                                       hasProperty("displayContext", is("READONLY"))))),
                () -> assertThat(actual.getCaseFields(), hasItem(allOf(hasProperty("id", is("Addresses2")),
                                                                       not(hasProperty("displayContext", is("READONLY"))))))
            );
        }
    }

    @Nested
    @DisplayName("case event definitions ACL tests")
    class ReturnsCaseEventsDataWithCaseEventAccess_AclTests {

        @Test
        @DisplayName("Should not return case event definition if relevant acl missing")
        void shouldNotReturnCaseEventDefinitionIfRelevantAclMissing() throws IOException {
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
                       is(emptyCollectionOf(CaseEvent.class)));
        }

        @Test
        @DisplayName("Should not return case event definition if relevant acl not granting access")
        void shouldNotReturnCaseEventDefinitionIfRelevantAclNotGrantingAccess() throws IOException {
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
                       is(emptyCollectionOf(CaseEvent.class)));
        }

        @Test
        @DisplayName("Should return case event definition if acl matching")
        void shouldReturnCaseEventDefinitionWithAclMatching() throws IOException {
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
                               .withId(EVENT_ID)
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .build())
                .build();

            List<CaseEvent> result = accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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

            List<CaseEvent> result = accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
                               .withId(EVENT_ID_WITH_ACCESS)
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .withAcl(anAcl()
                                            .withRole(ROLE_NOT_IN_USER_ROLES)
                                            .build())
                           .build())
                .withEvent(anCaseEvent().withId(EVENT_ID_WITHOUT_ACCESS)
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_3)
                                            .build())
                           .build())
                .withEvent(anCaseEvent()
                               .withId(EVENT_ID_WITHOUT_ACCESS_2)
                               .withAcl(anAcl()
                                            .withRole(ROLE_NOT_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                           .build())
                .withEvent(anCaseEvent()
                               .withId(EVENT_ID_WITH_ACCESS_2)
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_2)
                                            .withCreate(true)
                                            .build())
                           .build())
                .build();

            List<CaseEvent> result = accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
                       is(emptyCollectionOf(CaseEvent.class)));
        }

        @Test
        @DisplayName("Should not return case event definition if relevant acl not granting access")
        void shouldNotReturnCaseEventDefinitionIfRelevantAclNotGrantingAccess() throws IOException {
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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
                       is(emptyCollectionOf(CaseEvent.class)));
        }

        @Test
        @DisplayName("Should return case event definition if acl matching")
        void shouldReturnCaseEventDefinitionWithAclMatching() throws IOException {
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
                               .withId(EVENT_ID)
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .build())
                .build();

            List<CaseEvent> result = accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
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

            List<CaseEvent> result = accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
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
            final CaseType caseType = newCaseType()
                .withEvent(anCaseEvent()
                               .withId(EVENT_ID_WITH_ACCESS)
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                               .withAcl(anAcl()
                                            .withRole(ROLE_NOT_IN_USER_ROLES)
                                            .build())
                           .build())
                .withEvent(anCaseEvent().withId(EVENT_ID_WITHOUT_ACCESS)
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_3)
                                            .build())
                           .build())
                .withEvent(anCaseEvent()
                               .withId(EVENT_ID_WITHOUT_ACCESS_2)
                               .withAcl(anAcl()
                                            .withRole(ROLE_NOT_IN_USER_ROLES)
                                            .withCreate(true)
                                            .build())
                           .build())
                .withEvent(anCaseEvent()
                               .withId(EVENT_ID_WITH_ACCESS_2)
                               .withAcl(anAcl()
                                            .withRole(ROLE_IN_USER_ROLES_2)
                                            .withCreate(true)
                                            .build())
                           .build())
                .build();

            List<CaseEvent> result = accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
                                                                                   USER_ROLES,
                                                                                   CAN_CREATE);
            assertAll(
                () -> assertThat(result, hasSize(2)),
                () -> assertThat(result, hasItem(hasProperty("id", is("EVENT_ID_WITH_ACCESS")))),
                () -> assertThat(result, hasItem(hasProperty("id", is("EVENT_ID_WITH_ACCESS_2"))))
            );
        }
    }

    private JsonNode getJsonNode(String content) throws IOException {
        final Map<String, JsonNode> newData = MAPPER.convertValue(MAPPER.readTree(content), STRING_JSON_MAP);
        return MAPPER.convertValue(newData, JsonNode.class);
    }

    private void assertFieldsAccess(boolean hasFieldAccess, CaseType caseType, JsonNode newDataNode, JsonNode existingDataNode) {
        assertThat(
            accessControlService.canAccessCaseFieldsForUpsert(
                newDataNode,
                existingDataNode,
                caseType.getCaseFields(),
                USER_ROLES),
            is(hasFieldAccess));
    }

    private JsonNode getTextNode(String value) {
        return JSON_NODE_FACTORY.textNode(value);
    }
}
