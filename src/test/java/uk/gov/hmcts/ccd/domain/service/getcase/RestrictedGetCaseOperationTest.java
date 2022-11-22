package uk.gov.hmcts.ccd.domain.service.getcase;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

class RestrictedGetCaseOperationTest {
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String USER_ID = "26";
    private static final String CASE_REFERENCE = "1234123412341234";
    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA3 = "caseworker-probate-loa3";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";

    @Mock
    private DefaultGetCaseOperation defaultGetCaseOperation;
    @Mock
    private GetCaseOperation authorisedGetCaseOperation;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CaseUserRepository caseUserRepository;
    @Mock
    private CaseDataAccessControl caseDataAccessControl;
    @Mock
    private AccessControlService accessControlService;

    private RestrictedGetCaseOperation restrictedGetCaseOperation;
    private CaseDetails caseDetails;
    private final CaseTypeDefinition caseType = new CaseTypeDefinition();
    private final Set<String> userRoles = Sets.newHashSet(CASEWORKER_DIVORCE, CASEWORKER_PROBATE_LOA1,
        CASEWORKER_PROBATE_LOA3);
    private final List<String> caseRoles = Collections.emptyList();
    private final Set<AccessProfile> accessProfiles = createAccessProfiles(userRoles);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setState("Some State");
        caseDetails.setId(CASE_REFERENCE);
        Optional<CaseDetails> caseDetailsOptional = Optional.of(caseDetails);
        doReturn(caseDetailsOptional).when(authorisedGetCaseOperation).execute(JURISDICTION_ID, CASE_TYPE_ID,
            CASE_REFERENCE);
        doReturn(caseDetailsOptional).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);
        doReturn(caseDetailsOptional).when(defaultGetCaseOperation).execute(JURISDICTION_ID, CASE_TYPE_ID,
            CASE_REFERENCE);
        doReturn(caseDetailsOptional).when(defaultGetCaseOperation).execute(CASE_REFERENCE);

        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria((caseType), (accessProfiles),
            (CAN_READ));
        doReturn(true).when(accessControlService).canAccessCaseStateWithCriteria((caseDetails.getState()),
            (caseType), (accessProfiles), (CAN_READ));
        doReturn(caseType).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        when(caseDataAccessControl.generateAccessProfilesForRestrictedCase(any(CaseDetails.class)))
            .thenReturn(accessProfiles);

        doReturn(USER_ID).when(userRepository).getUserId();
        doReturn(caseRoles).when(caseUserRepository).findCaseRoles(Long.valueOf(CASE_REFERENCE), USER_ID);
        restrictedGetCaseOperation = new RestrictedGetCaseOperation(defaultGetCaseOperation,
            authorisedGetCaseOperation,
            caseDefinitionRepository,
            caseDataAccessControl,
            accessControlService
            );
    }

    @Nested
    @DisplayName("execute(jurisdictionId, caseTypeId, caseReference)")
    class ExecuteJurisdictionCaseTypeReference {
        @Test
        @DisplayName("should call decorated implementation")
        void shouldCallDecoratedImplementation() {
            restrictedGetCaseOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

            verify(authorisedGetCaseOperation).execute(CASE_REFERENCE);
        }

        @Test
        @DisplayName("should return empty optional when case not found")
        void shouldReturnEmptyOptionalWhenCaseNotFound() {
            doReturn(Optional.empty()).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);
            doReturn(Optional.empty()).when(defaultGetCaseOperation).execute(CASE_REFERENCE);

            final Optional<CaseDetails> output = restrictedGetCaseOperation.execute(JURISDICTION_ID,
                CASE_TYPE_ID,
                CASE_REFERENCE);

            assertAll(
                () -> assertThat(output.isPresent(), is(false)),
                () -> verify(accessControlService, never()).canAccessCaseTypeWithCriteria(any(), any(), any()),
                () -> verify(accessControlService, never()).filterCaseFieldsByAccess(any(), any(), any(), any(),
                    anyBoolean())
            );
        }

        @Test
        @DisplayName("should error when unauthorised and with restricted access")
        void shouldErrorWhenUnauthorisedAndRestrictedAccess() {
            doReturn(Optional.empty()).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);

            assertThrows(ForbiddenException.class, () -> restrictedGetCaseOperation.execute(JURISDICTION_ID,
                CASE_TYPE_ID,
                CASE_REFERENCE));
        }

        @Test
        @DisplayName("should pass through case details when case authorised")
        void shouldPassThroughCaseDetailsWhenCaseAuthorised() {
            final Optional<CaseDetails> result = restrictedGetCaseOperation.execute(JURISDICTION_ID,
                CASE_TYPE_ID,
                CASE_REFERENCE);

            assertAll(
                () -> assertTrue(result.isPresent()),
                () -> assertThat(result.get(), is(caseDetails)),
                () -> verify(authorisedGetCaseOperation).execute(CASE_REFERENCE),
                () -> verifyNoInteractions(defaultGetCaseOperation, caseDefinitionRepository,
                    caseDataAccessControl, accessControlService)
            );
        }

        @Test
        @DisplayName("should return empty case if no case type found")
        void shouldReturnEmptyCaseIfNoCaseTypeFound() {
            doReturn(Optional.empty()).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);
            doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

            final Optional<CaseDetails> result = restrictedGetCaseOperation.execute(JURISDICTION_ID,
                CASE_TYPE_ID,
                CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                authorisedGetCaseOperation, accessControlService, caseDataAccessControl);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(caseDataAccessControl).generateAccessProfilesForRestrictedCase(caseDetails),
                () -> inOrder.verify(accessControlService, never()).canAccessCaseTypeWithCriteria((caseType),
                    (accessProfiles), (CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        @DisplayName("should return empty case if no user roles found")
        void shouldReturnEmptyCaseIfNoUserRolesFound() {
            doReturn(Optional.empty()).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);
            when(caseDataAccessControl.generateAccessProfilesForRestrictedCase(any(CaseDetails.class)))
                .thenReturn(Sets.newHashSet());

            final Optional<CaseDetails> result = restrictedGetCaseOperation.execute(JURISDICTION_ID,
                CASE_TYPE_ID,
                CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                authorisedGetCaseOperation, accessControlService, caseDataAccessControl);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(caseDataAccessControl).generateAccessProfilesForRestrictedCase(caseDetails),
                () -> inOrder.verify(accessControlService, never())
                    .canAccessCaseTypeWithCriteria((caseType), (accessProfiles), (CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        @DisplayName("should return empty case if empty user roles")
        void shouldReturnEmptyCaseIfEmptyUserRolesFound() {
            doReturn(Optional.empty()).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);
            when(caseDataAccessControl.generateAccessProfilesForRestrictedCase(any(CaseDetails.class)))
                .thenReturn(Sets.newHashSet());

            final Optional<CaseDetails> result = restrictedGetCaseOperation.execute(JURISDICTION_ID,
                CASE_TYPE_ID,
                CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                authorisedGetCaseOperation, accessControlService, caseDataAccessControl);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(caseDataAccessControl).generateAccessProfilesForRestrictedCase(caseDetails),
                () -> inOrder.verify(accessControlService, never()).canAccessCaseTypeWithCriteria((caseType),
                    (accessProfiles), (CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        @DisplayName("should return empty case if no case type read access")
        void shouldReturnEmptyCaseIfNoCaseTypeReadAccess() {
            doReturn(Optional.empty()).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);
            doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria((caseType), (accessProfiles),
                (CAN_READ));

            final Optional<CaseDetails> result = restrictedGetCaseOperation.execute(JURISDICTION_ID,
                CASE_TYPE_ID,
                CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                authorisedGetCaseOperation, accessControlService, caseDataAccessControl);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(caseDataAccessControl).generateAccessProfilesForRestrictedCase(caseDetails),
                () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria((caseType),
                    (accessProfiles),
                    (CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }
    }

    @Nested
    @DisplayName("execute(caseReference)")
    class ExecuteReference {
        @Test
        @DisplayName("should call decorated implementation")
        void shouldCallDecoratedImplementation() {
            restrictedGetCaseOperation.execute(CASE_REFERENCE);

            verify(authorisedGetCaseOperation).execute(CASE_REFERENCE);
        }

        @Test
        @DisplayName("should return empty optional when case not found")
        void shouldReturnEmptyOptionalWhenCaseNotFound() {
            doReturn(Optional.empty()).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);
            doReturn(Optional.empty()).when(defaultGetCaseOperation).execute(CASE_REFERENCE);

            final Optional<CaseDetails> output = restrictedGetCaseOperation.execute(CASE_REFERENCE);

            assertAll(
                () -> assertThat(output.isPresent(), is(false)),
                () -> verify(accessControlService, never()).canAccessCaseTypeWithCriteria(any(), any(), any()),
                () -> verify(accessControlService, never()).filterCaseFieldsByAccess(any(), any(), any(), any(),
                    anyBoolean())
            );
        }

        @Test
        @DisplayName("should error when unauthorised and with restricted access")
        void shouldErrorWhenUnauthorisedAndRestrictedAccess() {
            doReturn(Optional.empty()).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);

            assertThrows(ForbiddenException.class, () -> restrictedGetCaseOperation.execute(CASE_REFERENCE));
        }

        @Test
        @DisplayName("should pass through case details when case authorised")
        void shouldPassThroughCaseDetailsWhenCaseAuthorised() {
            final Optional<CaseDetails> result = restrictedGetCaseOperation.execute(CASE_REFERENCE);

            assertAll(
                () -> assertTrue(result.isPresent()),
                () -> assertThat(result.get(), is(caseDetails)),
                () -> verify(authorisedGetCaseOperation).execute(CASE_REFERENCE),
                () -> verifyNoInteractions(defaultGetCaseOperation, caseDefinitionRepository,
                    caseDataAccessControl, accessControlService)
            );
        }

        @Test
        @DisplayName("should return empty case if no case type found")
        void shouldReturnEmptyCaseIfNoCaseTypeFound() {
            doReturn(Optional.empty()).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);
            doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

            final Optional<CaseDetails> result = restrictedGetCaseOperation.execute(CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                authorisedGetCaseOperation, accessControlService, caseDataAccessControl);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(caseDataAccessControl).generateAccessProfilesForRestrictedCase(caseDetails),
                () -> inOrder.verify(accessControlService, never()).canAccessCaseTypeWithCriteria((caseType),
                    (accessProfiles), (CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        @DisplayName("should return empty case if no user roles found")
        void shouldReturnEmptyCaseIfNoUserRolesFound() {
            doReturn(Optional.empty()).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);
            when(caseDataAccessControl.generateAccessProfilesForRestrictedCase(any(CaseDetails.class)))
                .thenReturn(Sets.newHashSet());

            final Optional<CaseDetails> result = restrictedGetCaseOperation.execute(CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                authorisedGetCaseOperation, accessControlService, caseDataAccessControl);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(caseDataAccessControl).generateAccessProfilesForRestrictedCase(caseDetails),
                () -> inOrder.verify(accessControlService, never())
                    .canAccessCaseTypeWithCriteria((caseType), (accessProfiles), (CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        @DisplayName("should return empty case if empty user roles")
        void shouldReturnEmptyCaseIfEmptyUserRolesFound() {
            doReturn(Optional.empty()).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);
            when(caseDataAccessControl.generateAccessProfilesForRestrictedCase(any(CaseDetails.class)))
                .thenReturn(Sets.newHashSet());

            final Optional<CaseDetails> result = restrictedGetCaseOperation.execute(CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                authorisedGetCaseOperation, accessControlService, caseDataAccessControl);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(caseDataAccessControl).generateAccessProfilesForRestrictedCase(caseDetails),
                () -> inOrder.verify(accessControlService, never()).canAccessCaseTypeWithCriteria((caseType),
                    (accessProfiles), (CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        @DisplayName("should return empty case if no case type read access")
        void shouldReturnEmptyCaseIfNoCaseTypeReadAccess() {
            doReturn(Optional.empty()).when(authorisedGetCaseOperation).execute(CASE_REFERENCE);
            doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria((caseType), (accessProfiles),
                (CAN_READ));

            final Optional<CaseDetails> result = restrictedGetCaseOperation.execute(CASE_REFERENCE);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                authorisedGetCaseOperation, accessControlService, caseDataAccessControl);
            assertAll(
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(caseDataAccessControl).generateAccessProfilesForRestrictedCase(caseDetails),
                () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria((caseType),
                    (accessProfiles),
                    (CAN_READ)),
                () -> assertThat(result.isPresent(), is(false))
            );
        }
    }


    private Set<AccessProfile> createAccessProfiles(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> AccessProfile.builder().readOnly(false)
                .accessProfile(userRole)
                .build())
            .collect(Collectors.toSet());
    }
}
