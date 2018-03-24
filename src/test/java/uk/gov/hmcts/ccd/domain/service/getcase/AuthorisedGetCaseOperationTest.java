package uk.gov.hmcts.ccd.domain.service.getcase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

class AuthorisedGetCaseOperationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String CASE_REFERENCE = "1234123412341234";
    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA3 = "caseworker-probate-loa3";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";

    @Mock
    private GetCaseOperation classifiedGetCaseOperation;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private UserRepository userRepository;

    @Mock
    private AccessControlService accessControlService;

    private AuthorisedGetCaseOperation authorisedGetCaseOperation;
    private CaseDetails caseDetails;
    private final CaseType caseType = new CaseType();
    private final Set<String> userRoles = Sets.newHashSet(CASEWORKER_DIVORCE, CASEWORKER_PROBATE_LOA1, CASEWORKER_PROBATE_LOA3);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setState("Some State");
        Optional<CaseDetails> caseDetailsOptional = Optional.of(caseDetails);
        JsonNode filteredDataNode = JSON_NODE_FACTORY.objectNode();
        JsonNode testValueNode = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode)filteredDataNode).set("testField", testValueNode);
        doReturn(caseDetailsOptional).when(classifiedGetCaseOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);
        doReturn(caseDetailsOptional).when(classifiedGetCaseOperation).execute(CASE_REFERENCE);

        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ));
        doReturn(filteredDataNode).when(accessControlService).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseType.getCaseFields()), eq(userRoles), eq(CAN_READ));
        doReturn(true).when(accessControlService).canAccessCaseStateWithCriteria(eq(caseDetails.getState()), eq(caseType), eq(userRoles), eq(CAN_READ));
        doReturn(caseType).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(userRoles).when(userRepository).getUserRoles();
        authorisedGetCaseOperation = new AuthorisedGetCaseOperation(classifiedGetCaseOperation,
                                                                    caseDefinitionRepository, accessControlService, userRepository);
    }

    @Nested
    @DisplayName("execute(jurisdictionId, caseTypeId, caseReference)")
    class execute_jurisdictionCaseTypeReference {
        @Test
        @DisplayName("should call decorated implementation")
        void shouldCallDecoratedImplementation() {
            authorisedGetCaseOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

            verify(classifiedGetCaseOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);
        }

        @Test
        @DisplayName("should return empty optional when case not found")
        void shouldReturnEmptyOptionalWhenCaseNotFound() {
            doReturn(Optional.empty()).when(classifiedGetCaseOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

            final Optional<CaseDetails> output = authorisedGetCaseOperation.execute(JURISDICTION_ID,
                                                                                    CASE_TYPE_ID,
                                                                                    CASE_REFERENCE);

            assertAll(
                () -> assertThat(output.isPresent(), is(false)),
                () -> verify(accessControlService, never()).canAccessCaseTypeWithCriteria(any(), any(), any()),
                () -> verify(accessControlService, never()).filterCaseFieldsByAccess(any(), any(), any(), any())
            );
        }

        @Test
        @DisplayName("should apply authorization when case found")
        void shouldApplyAuthorizationWhenCaseFound() {
            final Optional<CaseDetails> result = authorisedGetCaseOperation.execute(JURISDICTION_ID,
                                                                                    CASE_TYPE_ID,
                                                                                    CASE_REFERENCE);

            JsonNode resultNode = MAPPER.convertValue(caseDetails.getData(), JsonNode.class);
            InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, classifiedGetCaseOperation, accessControlService);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(userRepository).getUserRoles(),
                () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)),
                () -> inOrder.verify(accessControlService).canAccessCaseStateWithCriteria(eq(caseDetails.getState()), eq(caseType), eq(userRoles), eq(CAN_READ)),
                () -> inOrder.verify(accessControlService, times(2)).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseType.getCaseFields()), eq(userRoles), eq(CAN_READ)),
                () -> assertThat(result.get(), sameInstance(caseDetails)),
                () -> assertThat(resultNode.has("testField"), is(true)));
        }

        @Test
        @DisplayName("should return empty case if no case type found")
        void shouldReturnEmptyCaseIfNoCaseTypeFound() {
            doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

            final Optional<CaseDetails> result = authorisedGetCaseOperation.execute(JURISDICTION_ID,
                                                                                    CASE_TYPE_ID,
                                                                                    CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, classifiedGetCaseOperation, accessControlService);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(userRepository).getUserRoles(),
                () -> inOrder.verify(accessControlService, never()).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)),
                () -> inOrder.verify(accessControlService, never()).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseType.getCaseFields()), eq(userRoles), eq(CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        @DisplayName("should return empty case if no user roles found")
        void shouldReturnEmptyCaseIfNoUserRolesFound() {
            doReturn(null).when(userRepository).getUserRoles();

            final Optional<CaseDetails> result = authorisedGetCaseOperation.execute(JURISDICTION_ID,
                                                                                    CASE_TYPE_ID,
                                                                                    CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, classifiedGetCaseOperation, accessControlService);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(userRepository).getUserRoles(),
                () -> inOrder.verify(accessControlService, never()).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)),
                () -> inOrder.verify(accessControlService, never()).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseType.getCaseFields()), eq(userRoles), eq(CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        @DisplayName("should return empty case if empty user roles")
        void shouldReturnEmptyCaseIfEmptyUserRolesFound() {
            doReturn(Sets.newHashSet()).when(userRepository).getUserRoles();

            final Optional<CaseDetails> result = authorisedGetCaseOperation.execute(JURISDICTION_ID,
                                                                                    CASE_TYPE_ID,
                                                                                    CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, classifiedGetCaseOperation, accessControlService);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(userRepository).getUserRoles(),
                () -> inOrder.verify(accessControlService, never()).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)),
                () -> inOrder.verify(accessControlService, never()).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseType.getCaseFields()), eq(userRoles), eq(CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        @DisplayName("should return empty case if no case type read access")
        void shouldReturnEmptyCaseIfNoCaseTypeReadAccess() {
            doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ));

            final Optional<CaseDetails> result = authorisedGetCaseOperation.execute(JURISDICTION_ID,
                                                                                    CASE_TYPE_ID,
                                                                                    CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, classifiedGetCaseOperation, accessControlService);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(userRepository).getUserRoles(),
                () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)),
                () -> inOrder.verify(accessControlService, never()).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseType.getCaseFields()), eq(userRoles), eq(CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

    }

    @Nested
    @DisplayName("execute(caseReference)")
    class execute_reference {
        @Test
        @DisplayName("should call decorated implementation")
        void shouldCallDecoratedImplementation() {
            authorisedGetCaseOperation.execute(CASE_REFERENCE);

            verify(classifiedGetCaseOperation).execute(CASE_REFERENCE);
        }

        @Test
        @DisplayName("should return empty optional when case not found")
        void shouldReturnEmptyOptionalWhenCaseNotFound() {
            doReturn(Optional.empty()).when(classifiedGetCaseOperation).execute(CASE_REFERENCE);

            final Optional<CaseDetails> output = authorisedGetCaseOperation.execute(CASE_REFERENCE);

            assertAll(
                () -> assertThat(output.isPresent(), is(false)),
                () -> verify(accessControlService, never()).canAccessCaseTypeWithCriteria(any(), any(), any()),
                () -> verify(accessControlService, never()).filterCaseFieldsByAccess(any(), any(), any())
            );
        }


        @Test
        @DisplayName("should apply authorization when case found")
        void shouldApplyAuthorizationWhenCaseFound() {
            final Optional<CaseDetails> result = authorisedGetCaseOperation.execute(CASE_REFERENCE);

            JsonNode resultNode = MAPPER.convertValue(caseDetails.getData(), JsonNode.class);
            InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, classifiedGetCaseOperation, accessControlService);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(userRepository).getUserRoles(),
                () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)),
                () -> inOrder.verify(accessControlService).canAccessCaseStateWithCriteria(eq(caseDetails.getState()), eq(caseType), eq(userRoles), eq(CAN_READ)),
                () -> inOrder.verify(accessControlService, times(2)).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseType.getCaseFields()), eq(userRoles), eq(CAN_READ)),
                () -> assertThat(result.get(), sameInstance(caseDetails)),
                () -> assertThat(resultNode.has("testField"), is(true)));
        }

        @Test
        @DisplayName("should return empty case if no case type found")
        void shouldReturnEmptyCaseIfNoCaseTypeFound() {
            doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

            final Optional<CaseDetails> result = authorisedGetCaseOperation.execute(CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, classifiedGetCaseOperation, accessControlService);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(userRepository).getUserRoles(),
                () -> inOrder.verify(accessControlService, never()).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)),
                () -> inOrder.verify(accessControlService, never()).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseType.getCaseFields()), eq(userRoles), eq(CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        @DisplayName("should return empty case if no user roles found")
        void shouldReturnEmptyCaseIfNoUserRolesFound() {
            doReturn(null).when(userRepository).getUserRoles();

            final Optional<CaseDetails> result = authorisedGetCaseOperation.execute(CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, classifiedGetCaseOperation, accessControlService);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(userRepository).getUserRoles(),
                () -> inOrder.verify(accessControlService, never()).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)),
                () -> inOrder.verify(accessControlService, never()).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseType.getCaseFields()), eq(userRoles), eq(CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        @DisplayName("should return empty case if empty user roles")
        void shouldReturnEmptyCaseIfEmptyUserRolesFound() {
            doReturn(Sets.newHashSet()).when(userRepository).getUserRoles();

            final Optional<CaseDetails> result = authorisedGetCaseOperation.execute(CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, classifiedGetCaseOperation, accessControlService);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(userRepository).getUserRoles(),
                () -> inOrder.verify(accessControlService, never()).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)),
                () -> inOrder.verify(accessControlService, never()).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseType.getCaseFields()), eq(userRoles), eq(CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        @DisplayName("should return empty case if no case type read access")
        void shouldReturnEmptyCaseIfNoCaseTypeReadAccess() {
            doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ));

            final Optional<CaseDetails> result = authorisedGetCaseOperation.execute(CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository, userRepository, classifiedGetCaseOperation, accessControlService);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(userRepository).getUserRoles(),
                () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType), eq(userRoles), eq(CAN_READ)),
                () -> inOrder.verify(accessControlService, never()).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseType.getCaseFields()), eq(userRoles), eq(CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }
    }
}
