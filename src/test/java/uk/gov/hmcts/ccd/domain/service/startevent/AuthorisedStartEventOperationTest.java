package uk.gov.hmcts.ccd.domain.service.startevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;

class AuthorisedStartEventOperationTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String CASE_REFERENCE = "1234123412341234";
    private static final String EVENT_TRIGGER_ID = "updateEvent";
    private static final Boolean IGNORE_WARNING = Boolean.TRUE;

    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA3 = "caseworker-probate-loa3";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";
    private static final Map<String, JsonNode> EMPTY_MAP = Maps.newHashMap();


    @Mock
    private StartEventOperation classifiedStartEventOperation;

    @Mock
    private AccessControlService accessControlService;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private CaseDetailsRepository caseDetailsRepository;
    @Mock
    private AuthorisedStartEventOperation authorisedStartEventOperation;
    @Mock
    private DraftGateway draftGateway;
    @Mock
    private CaseAccessService caseAccessService;

    @Mock
    private UIDService uidService;

    private Optional<CaseDetails> caseDetailsOptional;
    private CaseDetails classifiedCaseDetails;
    private JsonNode authorisedCaseDetailsNode;
    private JsonNode authorisedCaseDetailsClassificationNode;
    private JsonNode classifiedCaseDetailsNode;
    private JsonNode classifiedCaseDetailsClassificationNode;
    private StartEventResult classifiedStartEvent;
    private final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
    private final List<CaseFieldDefinition> caseFieldDefinitions = Lists.newArrayList();
    private final Set<String> userRoles = Sets.newHashSet(CASEWORKER_DIVORCE,
        CASEWORKER_PROBATE_LOA1,
        CASEWORKER_PROBATE_LOA3,
        GlobalCaseRole.CREATOR.getRole());

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        classifiedCaseDetailsNode = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) classifiedCaseDetailsNode).put("dataTestField", "dataTestValue");
        ((ObjectNode) classifiedCaseDetailsNode).put("dataTestField2", "dataTestValue2");
        classifiedCaseDetailsClassificationNode = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) classifiedCaseDetailsClassificationNode).put("classificationTestField",
            "classificationTestValue");
        ((ObjectNode) classifiedCaseDetailsClassificationNode).put("classificationTestField2",
            "classificationTestValue2");

        authorisedCaseDetailsNode = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) authorisedCaseDetailsNode).put("dataTestField", "dataTestValue");
        authorisedCaseDetailsClassificationNode = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) authorisedCaseDetailsClassificationNode).put("classificationTestField",
            "classificationTestValue");

        classifiedCaseDetails = new CaseDetails();
        classifiedCaseDetails.setData(JacksonUtils.convertValue(classifiedCaseDetailsNode));
        classifiedCaseDetails.setDataClassification(JacksonUtils.convertValue(classifiedCaseDetailsClassificationNode));
        classifiedStartEvent = new StartEventResult();
        classifiedStartEvent.setCaseDetails(classifiedCaseDetails);

        caseDetailsOptional = Optional.of(newCaseDetails().withCaseTypeId(CASE_TYPE_ID).build());

        authorisedStartEventOperation = new AuthorisedStartEventOperation(classifiedStartEventOperation,
            caseDefinitionRepository,
            caseDetailsRepository,
            accessControlService,
            uidService,
            draftGateway,
            caseAccessService);
        caseTypeDefinition.setCaseFieldDefinitions(caseFieldDefinitions);
        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseTypeDefinition);
        when(caseAccessService.getUserRoles()).thenReturn(userRoles);
        when(accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, userRoles, CAN_READ)).thenReturn(true);
        when(accessControlService.filterCaseFieldsByAccess(eq(classifiedCaseDetailsNode),
            eq(caseFieldDefinitions),
            eq(userRoles),
            eq(CAN_READ),
            anyBoolean())).thenReturn(authorisedCaseDetailsNode);
        when(accessControlService.filterCaseFieldsByAccess(eq(classifiedCaseDetailsClassificationNode),
            eq(caseFieldDefinitions),
            eq(userRoles),
            eq(CAN_READ), anyBoolean())).thenReturn(
            authorisedCaseDetailsClassificationNode);
        when(uidService.validateUID(anyString())).thenReturn(true);
    }

    @Nested
    @DisplayName("for case type - deprecated")
    class ForCaseTypeDeprecated {

        @BeforeEach
        void setUp() {
            doReturn(classifiedStartEvent).when(classifiedStartEventOperation).triggerStartForCaseType(CASE_TYPE_ID,
                EVENT_TRIGGER_ID,
                IGNORE_WARNING);
            when(accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition,
                userRoles,
                CAN_CREATE)).thenReturn(true);
        }

        @Test
        @DisplayName("should call decorated start event operation as is")
        void shouldCallDecoratedStartEventOperation() {

            final StartEventResult output = authorisedStartEventOperation.triggerStartForCaseType(CASE_TYPE_ID,
                EVENT_TRIGGER_ID,
                IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(classifiedStartEvent)),
                () -> assertThat(output.getCaseDetails(), sameInstance(classifiedCaseDetails)),
                () -> verify(classifiedStartEventOperation).triggerStartForCaseType(CASE_TYPE_ID,
                    EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
        }

        @Test
        @DisplayName("should filter out data when no case type read access")
        void shouldFilterOutDataWhenNoCaseTypeReadAccess() {

            when(accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, userRoles, CAN_READ)).thenReturn(false);

            final StartEventResult output = authorisedStartEventOperation.triggerStartForCaseType(CASE_TYPE_ID,
                EVENT_TRIGGER_ID,
                IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(classifiedStartEvent)),
                () -> assertThat(output.getCaseDetails().getData(), is(EMPTY_MAP)),
                () -> assertThat(output.getCaseDetails().getDataClassification(), is(EMPTY_MAP)),
                () -> verify(classifiedStartEventOperation).triggerStartForCaseType(CASE_TYPE_ID,
                    EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
        }
    }

    @Nested
    @DisplayName("for case type")
    class ForCaseType {

        @BeforeEach
        void setUp() {
            doReturn(classifiedStartEvent).when(classifiedStartEventOperation).triggerStartForCaseType(CASE_TYPE_ID,
                EVENT_TRIGGER_ID,
                IGNORE_WARNING);
            when(accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition,
                userRoles,
                CAN_CREATE)).thenReturn(true);
        }

        @Test
        @DisplayName("should call decorated start event operation as is")
        void shouldCallDecoratedStartEventOperation() {

            final StartEventResult output = authorisedStartEventOperation.triggerStartForCaseType(CASE_TYPE_ID,
                EVENT_TRIGGER_ID,
                IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(classifiedStartEvent)),
                () -> assertThat(output.getCaseDetails(), sameInstance(classifiedCaseDetails)),
                () -> verify(classifiedStartEventOperation).triggerStartForCaseType(CASE_TYPE_ID,
                    EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
        }

        @Test
        @DisplayName("should filter out data when no case type read access")
        void shouldFilterOutDataWhenNoCaseTypeReadAccess() {

            when(accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, userRoles, CAN_READ)).thenReturn(false);

            final StartEventResult output = authorisedStartEventOperation.triggerStartForCaseType(CASE_TYPE_ID,
                EVENT_TRIGGER_ID,
                IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(classifiedStartEvent)),
                () -> assertThat(output.getCaseDetails().getData(), is(EMPTY_MAP)),
                () -> assertThat(output.getCaseDetails().getDataClassification(), is(EMPTY_MAP)),
                () -> verify(classifiedStartEventOperation).triggerStartForCaseType(CASE_TYPE_ID,
                    EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
        }
    }

    @Nested
    @DisplayName("for case")
    class ForCase {

        @BeforeEach
        void setUp() {
            doReturn(classifiedStartEvent).when(classifiedStartEventOperation).triggerStartForCase(CASE_REFERENCE,
                EVENT_TRIGGER_ID,
                IGNORE_WARNING);
            doReturn(caseDetailsOptional).when(caseDetailsRepository).findByReference(CASE_REFERENCE);
        }

        @Test
        @DisplayName("should call decorated start event operation as is")
        void shouldCallDecoratedStartEventOperation() {

            StartEventResult output = authorisedStartEventOperation.triggerStartForCase(CASE_REFERENCE,
                EVENT_TRIGGER_ID,
                IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(classifiedStartEvent)),
                () -> assertThat(output.getCaseDetails(), sameInstance(classifiedCaseDetails)),
                () -> verify(classifiedStartEventOperation).triggerStartForCase(CASE_REFERENCE,
                    EVENT_TRIGGER_ID,
                    IGNORE_WARNING)
            );
        }

        @Test
        @DisplayName("should return event trigger as is when case details null")
        void shouldReturnEventTriggerWhenCaseDetailsNull() {
            classifiedStartEvent.setCaseDetails(null);

            final StartEventResult output = authorisedStartEventOperation.triggerStartForCase(CASE_REFERENCE,
                EVENT_TRIGGER_ID,
                IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(classifiedStartEvent)),
                () -> assertThat(output.getCaseDetails(), is(nullValue()))
            );
        }

        @Test
        @DisplayName("should return event trigger with classified case details when not empty")
        void shouldReturnEventTriggerWithClassifiedCaseDetails() {

            final StartEventResult output = authorisedStartEventOperation.triggerStartForCase(CASE_REFERENCE,
                EVENT_TRIGGER_ID,
                IGNORE_WARNING);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                caseAccessService,
                classifiedStartEventOperation,
                accessControlService,
                uidService,
                caseDetailsRepository);
            assertAll(
                () -> assertThat(output, sameInstance(classifiedStartEvent)),
                () -> assertThat(output.getCaseDetails(), sameInstance(classifiedCaseDetails)),
                () -> assertThat(output.getCaseDetails().getData(),
                    is(equalTo(JacksonUtils.convertValue(authorisedCaseDetailsNode)))),
                () -> assertThat(output.getCaseDetails().getDataClassification(),
                    is(equalTo(JacksonUtils.convertValue(authorisedCaseDetailsClassificationNode)))),
                () -> inOrder.verify(uidService).validateUID(CASE_REFERENCE),
                () -> inOrder.verify(caseDetailsRepository).findByReference(CASE_REFERENCE),
                () -> inOrder.verify(classifiedStartEventOperation).triggerStartForCase(CASE_REFERENCE,
                    EVENT_TRIGGER_ID,
                    IGNORE_WARNING),
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(caseAccessService).getUserRoles(),
                () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseTypeDefinition),
                    eq(userRoles),
                    eq(CAN_READ)),
                () -> inOrder.verify(accessControlService).filterCaseFieldsByAccess(eq(classifiedCaseDetailsNode),
                    eq(caseFieldDefinitions),
                    eq(userRoles),
                    eq(CAN_READ),
                    anyBoolean()),
                () -> inOrder.verify(accessControlService).filterCaseFieldsByAccess(eq(classifiedCaseDetailsClassificationNode),
                    eq(caseFieldDefinitions),
                    eq(userRoles),
                    eq(CAN_READ),
                    anyBoolean())
            );
        }

        @Test
        @DisplayName("should fail if case type not found")
        void shouldFailIfNoCaseTypeFound() {

            doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

            assertThrows(ValidationException.class, () -> authorisedStartEventOperation.triggerStartForCase(CASE_REFERENCE,
                EVENT_TRIGGER_ID,
                IGNORE_WARNING));
        }

    }
}
