package uk.gov.hmcts.ccd.domain.service.caseaccess;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.OngoingStubbing;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentsDeleteRequest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentCategoryService;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.InvalidCaseRoleException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CaseAccessOperationTest {

    private static final String JURISDICTION = "CMC";
    private static final String WRONG_JURISDICTION = "DIVORCE";
    private static final String CASE_TYPE_ID = "Application";
    private static final Long CASE_REFERENCE = 1234123412341236L;
    private static final Long CASE_REFERENCE_OTHER = 1111222233334444L;
    private static final String USER_ID = "123";
    private static final String USER_ID_OTHER = "USER_ID_OTHER";
    private static final Long CASE_ID = 456L;
    private static final Long CASE_ID_OTHER = 1234L;
    private static final Long CASE_NOT_FOUND = 9999999999999999L;
    private static final String NOT_CASE_ROLE = "NotACaseRole";
    private static final String CASE_ROLE = "[DEFENDANT]";
    private static final String CASE_ROLE_OTHER = "[OTHER]";
    private static final String CASE_ROLE_CREATOR = "[CREATOR]";
    private static final String CASE_ROLE_GRANTED = "[ALREADY_GRANTED]";
    private static final String ORGANISATION = "ORGANISATION";
    private static final String ORGANISATION_OTHER = "ORGANISATION_OTHER";

    @Mock(lenient = true)
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CaseUserRepository caseUserRepository;

    @Mock
    private CaseRoleRepository caseRoleRepository;

    @Mock
    private SupplementaryDataRepository supplementaryDataRepository;

    @Mock
    private RoleAssignmentService roleAssignmentService;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private RoleAssignmentCategoryService roleAssignmentCategoryService;

    @InjectMocks
    private uk.gov.hmcts.ccd.domain.service.caseaccess.CaseAccessOperation caseAccessOperation;

    @BeforeEach
    void setUp() {
        configureCaseRepository(JURISDICTION);
        when(applicationParams.getEnableCaseUsersDbSync()).thenReturn(true);
    }


    @Nested
    @DisplayName("grantAccess()")
    class GrantAccess {

        @Captor
        ArgumentCaptor<CaseDetails> caseDetailsCaptor;

        @Captor
        ArgumentCaptor<List<String>> caseReferencesCaptor;

        @Captor
        ArgumentCaptor<List<String>> userIdsCaptor;

        @Captor
        ArgumentCaptor<Set<String>> rolesCaptor;

        @Test
        @DisplayName("should grant access to user")
        void shouldGrantAccess() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            // ACT
            caseAccessOperation.grantAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            // ASSERT
            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CREATOR.getRole()),
                () -> verifyNoInteractions(roleAssignmentService)
            );
        }

        @Test
        @DisplayName("RA set to true, should grant access to user, if [CREATOR] not already granted")
        void shouldGrantAccessForRA_IfNotAlreadyGranted() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);
            when(roleAssignmentService.findRoleAssignmentsByCasesAndUsers(any(), any())).thenReturn(new ArrayList<>());

            // ACT
            caseAccessOperation.grantAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            // ASSERT
            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> {
                    verify(roleAssignmentService)
                        .findRoleAssignmentsByCasesAndUsers(caseReferencesCaptor.capture(), userIdsCaptor.capture());
                    List<String> caseReferences = caseReferencesCaptor.getValue();
                    List<String> userIds = userIdsCaptor.getValue();

                    assertAll(
                        () -> assertEquals(1, caseReferences.size()),
                        () -> assertEquals(1, userIds.size()),
                        () -> assertEquals(CASE_REFERENCE.toString(), caseReferences.get(0)),
                        () -> assertEquals(USER_ID, userIds.get(0))
                    );
                },
                () -> {
                    verify(roleAssignmentService).createCaseRoleAssignments(
                        caseDetailsCaptor.capture(),
                        eq(USER_ID),
                        rolesCaptor.capture(),
                        eq(false)
                    );

                    CaseDetails caseDetails = caseDetailsCaptor.getValue();
                    assertAll(
                        () -> assertEquals(CASE_ID.toString(), caseDetails.getId()),
                        () -> assertEquals(CASE_REFERENCE, caseDetails.getReference())
                    );

                    Set<String> roles = rolesCaptor.getValue();
                    assertAll(
                        () -> assertEquals(1, roles.size()),
                        () -> assertTrue(roles.contains(CREATOR.getRole()))
                    );
                },
                () -> verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CREATOR.getRole())
            );
        }

        @Test
        @DisplayName("RA set to true, db sync false, should grant access to user, if [CREATOR] not already granted")
        void shouldGrantAccessForRAWithoutDbSync_IfNotAlreadyGranted() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);
            when(applicationParams.getEnableCaseUsersDbSync()).thenReturn(false);
            when(roleAssignmentService.findRoleAssignmentsByCasesAndUsers(any(), any())).thenReturn(new ArrayList<>());

            // ACT
            caseAccessOperation.grantAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            // ASSERT
            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> {
                    verify(roleAssignmentService)
                        .findRoleAssignmentsByCasesAndUsers(caseReferencesCaptor.capture(), userIdsCaptor.capture());
                    List<String> caseReferences = caseReferencesCaptor.getValue();
                    List<String> userIds = userIdsCaptor.getValue();

                    assertAll(
                        () -> assertEquals(1, caseReferences.size()),
                        () -> assertEquals(1, userIds.size()),
                        () -> assertEquals(CASE_REFERENCE.toString(), caseReferences.get(0)),
                        () -> assertEquals(USER_ID, userIds.get(0))
                    );
                },
                () -> {
                    verify(roleAssignmentService).createCaseRoleAssignments(
                        caseDetailsCaptor.capture(),
                        eq(USER_ID),
                        rolesCaptor.capture(),
                        eq(false)
                    );

                    CaseDetails caseDetails = caseDetailsCaptor.getValue();
                    assertAll(
                        () -> assertEquals(CASE_ID.toString(), caseDetails.getId()),
                        () -> assertEquals(CASE_REFERENCE, caseDetails.getReference())
                    );

                    Set<String> roles = rolesCaptor.getValue();
                    assertAll(
                        () -> assertEquals(1, roles.size()),
                        () -> assertTrue(roles.contains(CREATOR.getRole()))
                    );
                },
                () -> verifyNoInteractions(caseUserRepository)
            );
        }

        @Test
        @DisplayName("RA set to true, should skip grant access to user, if [CREATOR] already granted")
        void shouldSkipGrantAccessForRA_IfAlreadyGranted() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);
            when(roleAssignmentService.findRoleAssignmentsByCasesAndUsers(any(), any())).thenReturn(List.of(
                new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CREATOR.getRole())
            ));

            // ACT
            caseAccessOperation.grantAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            // ASSERT
            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> {
                    verify(roleAssignmentService)
                        .findRoleAssignmentsByCasesAndUsers(caseReferencesCaptor.capture(), userIdsCaptor.capture());

                    List<String> caseReferences = caseReferencesCaptor.getValue();
                    List<String> userIds = userIdsCaptor.getValue();

                    assertAll(
                        () -> assertEquals(1, caseReferences.size()),
                        () -> assertEquals(1, userIds.size()),
                        () -> assertEquals(CASE_REFERENCE.toString(), caseReferences.get(0)),
                        () -> assertEquals(USER_ID, userIds.get(0))
                    );
                },
                () -> verify(roleAssignmentService, never()).createCaseRoleAssignments(
                    any(CaseDetails.class),
                    anyString(),
                    any(),
                    anyBoolean()
                ),
                () -> verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CREATOR.getRole())
            );
        }

        @Test
        @DisplayName("should throw not found exception when reference not found")
        void shouldThrowNotFound() {
            String caseNotFound = CASE_NOT_FOUND.toString();

            assertAll(
                () -> assertThrows(CaseNotFoundException.class,
                    () -> caseAccessOperation.grantAccess(JURISDICTION, caseNotFound, USER_ID)),
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_NOT_FOUND),
                () -> verifyNoInteractions(caseUserRepository),
                () -> verifyNoInteractions(roleAssignmentService)
            );
        }

        @Test
        @DisplayName("should throw not found exception when reference in different jurisdiction")
        void shouldHandleWrongJurisdiction() {
            String caseReference = CASE_REFERENCE.toString();

            assertAll(
                () -> assertThrows(CaseNotFoundException.class,
                    () -> caseAccessOperation.grantAccess(WRONG_JURISDICTION, caseReference, USER_ID)),
                () -> verify(caseDetailsRepository).findByReference(WRONG_JURISDICTION, CASE_REFERENCE),
                () -> verifyNoInteractions(caseUserRepository),
                () -> verifyNoInteractions(roleAssignmentService)
            );
        }
    }


    @Nested()
    @DisplayName("updateUserAccess(reference, caseUser)")
    class GrantAccessCaseUser {

        private CaseDetails caseDetails;

        @Captor
        ArgumentCaptor<Set<String>> rolesCaptor;

        @BeforeEach
        void setUp() {
            when(caseRoleRepository.getCaseRoles(CASE_TYPE_ID)).thenReturn(Sets.newHashSet(CASE_ROLE,
                CASE_ROLE_CREATOR,
                CASE_ROLE_OTHER,
                CASE_ROLE_GRANTED));

            caseDetails = new CaseDetails();
            caseDetails.setId(CASE_ID.toString());
            caseDetails.setReference(CASE_REFERENCE);
            caseDetails.setCaseTypeId(CASE_TYPE_ID);
        }

        @Test
        @DisplayName("should reject update when it contains an unknown case role")
        void shouldRejectWhenUnknownCaseRoles() {
            final Executable execAccessUpdate = () -> caseAccessOperation.updateUserAccess(caseDetails,
                caseUser(NOT_CASE_ROLE));
            assertThrows(InvalidCaseRoleException.class, execAccessUpdate);
            verifyNoInteractions(caseUserRepository);
            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("should grant access when added case role valid")
        void shouldGrantAccessForCaseRole() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            // ACT
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

            // ASSERT
            verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CASE_ROLE);
        }

        @Test
        @DisplayName("RA set to true, should grant access when added case role valid")
        void shouldGrantAccessForCaseRoleForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            // ACT
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

            // ASSERT
            verify(roleAssignmentService).createCaseRoleAssignments(
                eq(caseDetails),
                eq(USER_ID),
                rolesCaptor.capture(),
                eq(true)
            );
            verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CASE_ROLE);

            // :: verify roles submitted
            Set<String> roles = rolesCaptor.getValue();
            assertAll(
                () -> assertEquals(1, roles.size()),
                () -> assertTrue(roles.contains(CASE_ROLE))
            );
        }

        @Test
        @DisplayName("RA set to true, db sync false, should grant access when added case role valid")
        void shouldGrantAccessForCaseRoleForRAWithDbSyncFalse() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);
            when(applicationParams.getEnableCaseUsersDbSync()).thenReturn(false);

            // ACT
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

            // ASSERT
            verify(roleAssignmentService).createCaseRoleAssignments(
                eq(caseDetails),
                eq(USER_ID),
                rolesCaptor.capture(),
                eq(true)
            );
            verifyNoInteractions(caseUserRepository);

            // :: verify roles submitted
            Set<String> roles = rolesCaptor.getValue();
            assertAll(
                () -> assertEquals(1, roles.size()),
                () -> assertTrue(roles.contains(CASE_ROLE))
            );
        }

        @Test
        @DisplayName("should grant access when added case roles contains global [CREATOR]")
        void shouldGrantAccessForCaseRoleCreator() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            // ACT
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE, CREATOR.getRole()));

            // ASSERT
            assertAll(
                () -> verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CASE_ROLE),
                () -> verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CREATOR.getRole()),
                () -> verifyNoInteractions(roleAssignmentService)
            );
        }

        @Test
        @DisplayName("RA set to true, should grant access when added case roles contains global [CREATOR]")
        void shouldGrantAccessForCaseRoleCreatorForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            // ACT
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE, CREATOR.getRole()));

            // ASSERT
            verify(roleAssignmentService).createCaseRoleAssignments(
                eq(caseDetails),
                eq(USER_ID),
                rolesCaptor.capture(),
                eq(true)
            );
            verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CASE_ROLE);
            verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CREATOR.getRole());

            // :: verify roles submitted
            Set<String> roles = rolesCaptor.getValue();
            assertAll(
                () -> assertEquals(2, roles.size()),
                () -> assertTrue(roles.contains(CASE_ROLE)),
                () -> assertTrue(roles.contains(CREATOR.getRole()))
            );
        }

        @Test
        @DisplayName("should revoke access for removed case roles")
        void shouldRevokeRemovedCaseRoles() {
            // NB: test not valid for 'Attribute Based Access Control' as :
            //     RAS submission with `replaceExisting = true` does not require us to revokeRemoved.

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);
            when(caseUserRepository.findCaseRoles(CASE_ID, USER_ID)).thenReturn(List.of(CASE_ROLE_GRANTED));

            // ACT
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

            // ASSERT
            assertAll(
                () -> verify(caseUserRepository).revokeAccess(CASE_ID, USER_ID, CASE_ROLE_GRANTED),
                () -> verifyNoInteractions(roleAssignmentService)
            );
        }

        @Test
        @DisplayName("should ignore case roles already granted")
        void shouldIgnoreGrantedCaseRoles() {
            // NB: test not valid for 'Attribute Based Access Control' as :
            //     RAS submission with `replaceExisting = true` does not require us to ignoreGranted.

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);
            when(caseUserRepository.findCaseRoles(CASE_ID, USER_ID)).thenReturn(List.of(CASE_ROLE_GRANTED));

            // ACT
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE_GRANTED));

            // ASSERT
            assertAll(
                () -> verify(caseUserRepository, never()).grantAccess(CASE_ID, USER_ID, CASE_ROLE_GRANTED),
                () -> verifyNoInteractions(roleAssignmentService)
            );
        }
    }


    @Nested
    @DisplayName("revokeAccess()")
    class RevokeAccess {

        @Captor
        private ArgumentCaptor<List<RoleAssignmentsDeleteRequest>> deleteRequestsCaptor;

        @Test
        @DisplayName("should revoke access to user")
        void shouldRevokeAccess() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            // ACT
            caseAccessOperation.revokeAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            // ASSERT
            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> verify(caseUserRepository).revokeAccess(CASE_ID, USER_ID, CREATOR.getRole()),
                () -> verifyNoInteractions(roleAssignmentService)
            );
        }

        @Test
        @DisplayName("RA set to true, should revoke access to user")
        void shouldRevokeAccessForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            // ACT
            caseAccessOperation.revokeAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            // ASSERT
            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> {
                    verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture());

                    List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
                    assertAll(
                        () -> assertEquals(1, deleteRequests.size()),
                        () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                            CASE_REFERENCE.toString(), USER_ID, List.of(CREATOR.getRole()),
                            deleteRequests.get(0)
                        )
                    );
                },
                () -> verify(caseUserRepository).revokeAccess(CASE_ID, USER_ID, CREATOR.getRole())
            );
        }

        @Test
        @DisplayName("RA set to true, db sync false, should revoke access to user")
        void shouldRevokeAccessForRAWithDbSyncFalse() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);
            when(applicationParams.getEnableCaseUsersDbSync()).thenReturn(false);

            // ACT
            caseAccessOperation.revokeAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            // ASSERT
            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> {
                    verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture());

                    List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
                    assertAll(
                        () -> assertEquals(1, deleteRequests.size()),
                        () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                            CASE_REFERENCE.toString(), USER_ID, List.of(CREATOR.getRole()),
                            deleteRequests.get(0)
                        )
                    );
                },
                () -> verifyNoInteractions(caseUserRepository)
            );
        }

        @Test
        @DisplayName("should throw not found exception when reference not found")
        void shouldThrowNotFound() {
            String caseNotFound = CASE_NOT_FOUND.toString();

            assertAll(
                () -> assertThrows(CaseNotFoundException.class,
                    () -> caseAccessOperation.revokeAccess(JURISDICTION, caseNotFound, USER_ID)),
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_NOT_FOUND),
                () -> verifyNoInteractions(caseUserRepository),
                () -> verifyNoInteractions(roleAssignmentService)
            );
        }

        @Test
        @DisplayName("should throw not found exception when reference in different jurisdiction")
        void shouldHandleWrongJurisdiction() {
            String caseReference = CASE_REFERENCE.toString();

            assertAll(
                () -> assertThrows(CaseNotFoundException.class,
                    () -> caseAccessOperation.revokeAccess(WRONG_JURISDICTION, caseReference, USER_ID)),
                () -> verify(caseDetailsRepository).findByReference(WRONG_JURISDICTION, CASE_REFERENCE),
                () -> verifyNoInteractions(caseUserRepository),
                () -> verifyNoInteractions(roleAssignmentService)
            );
        }
    }


    @Nested
    @DisplayName("findCasesUserIdHasAccessTo(userId)")
    class FindCasesUserIdHasAccessTo {

        private final List<Long> ids = Collections.singletonList(1L);

        @Test
        @DisplayName("should return cases that the user has access to")
        void shouldReturnCasesUserHasAccessTo() {

            when(caseUserRepository.findCasesUserIdHasAccessTo(USER_ID))
                .thenReturn(ids);
            when(caseDetailsRepository.findCaseReferencesByIds(ids))
                .thenReturn(ids);

            List<String> usersCases = caseAccessOperation.findCasesUserIdHasAccessTo(USER_ID);

            assertAll(
                () -> assertEquals(1, usersCases.size()),
                () -> assertEquals(String.valueOf(1L), usersCases.get(0)),
                () -> verify(caseDetailsRepository, times(1)).findCaseReferencesByIds(ids)
            );
        }

        @Test
        @DisplayName("RA set to true, should return cases that the user has access to")
        void shouldReturnCasesUserHasAccessToForRA() {

            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);
            when(roleAssignmentService.getCaseReferencesForAGivenUser(USER_ID))
                .thenReturn(ids.stream().map(String::valueOf).collect(Collectors.toList()));

            List<String> usersCases = caseAccessOperation.findCasesUserIdHasAccessTo(USER_ID);

            assertAll(
                () -> assertEquals(1, usersCases.size()),
                () -> assertEquals(String.valueOf(1L), usersCases.get(0))
            );
        }

        @Test
        @DisplayName("should return zero cases if the user has no access to any cases")
        void shouldReturnEmptyListIfUserHasAccessToNoCases() {

            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);
            when(caseUserRepository.findCasesUserIdHasAccessTo(USER_ID))
                .thenReturn(new ArrayList<>());

            List<String> usersCases = caseAccessOperation.findCasesUserIdHasAccessTo(USER_ID);

            assertAll(
                () -> assertEquals(0, usersCases.size()),
                () -> verify(caseDetailsRepository, times(0)).findCaseReferencesByIds(ids)
            );
        }


        @Test
        @DisplayName("RA set to true, should return zero cases if the user has no access to any cases")
        void shouldReturnEmptyListIfUserHasAccessToNoCasesForRA() {

            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);
            when(roleAssignmentService.getCaseReferencesForAGivenUser(USER_ID))
                .thenReturn(new ArrayList<>());

            List<String> usersCases = caseAccessOperation.findCasesUserIdHasAccessTo(USER_ID);

            assertAll(
                () -> assertEquals(0, usersCases.size()),
                () -> verify(caseDetailsRepository, times(0)).findCaseReferencesByIds(ids)
            );
        }

    }


    @Nested()
    @DisplayName("addCaseUserRoles(caseUserRoles)")
    class AddCaseAssignUserRoles {

        @Captor
        ArgumentCaptor<CaseDetails> caseDetailsCaptor;

        @Captor
        ArgumentCaptor<Set<String>> rolesCaptor;

        @BeforeEach
        void setUp() {
            configureCaseRepository(null);
        }

        @Test
        @DisplayName("should throw not found exception when case not found")
        void shouldThrowNotFound() {

            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_NOT_FOUND.toString(), USER_ID, CASE_ROLE)
            );

            // ACT / ASSERT
            assertThrows(CaseNotFoundException.class, () -> caseAccessOperation.addCaseUserRoles(caseUserRoles));

            verifyNoInteractions(caseUserRepository);
            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("should add single case user role")
        void shouldAddSingleCaseUserRole() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
            );
            // behave as no existing case roles
            mockExistingCaseUserRoles(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID, USER_ID, CASE_ROLE);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should add single case user role")
        void shouldAddSingleCaseUserRoleForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
            );
            // behave as no existing case roles
            mockExistingCaseUserRolesForRA(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(roleAssignmentService).createCaseRoleAssignments(
                caseDetailsCaptor.capture(),
                eq(USER_ID),
                rolesCaptor.capture(),
                eq(false)
            );
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID, USER_ID, CASE_ROLE);

            CaseDetails caseDetails = caseDetailsCaptor.getValue();
            assertAll(
                () -> assertEquals(CASE_ID.toString(), caseDetails.getId()),
                () -> assertEquals(CASE_REFERENCE, caseDetails.getReference())
            );

            Set<String> roles = rolesCaptor.getValue();
            assertAll(
                () -> assertEquals(1, roles.size()),
                () -> assertTrue(roles.contains(CASE_ROLE))
            );
        }

        @Test
        @DisplayName("RA set to true, db sync false, should add single case user role")
        void shouldAddSingleCaseUserRoleForRAWithDbSyncFalse() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);
            when(applicationParams.getEnableCaseUsersDbSync()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
            );
            // behave as no existing case roles
            mockExistingCaseUserRolesForRA(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(roleAssignmentService).createCaseRoleAssignments(
                caseDetailsCaptor.capture(),
                eq(USER_ID),
                rolesCaptor.capture(),
                eq(false)
            );
            verifyNoInteractions(caseUserRepository);

            CaseDetails caseDetails = caseDetailsCaptor.getValue();
            assertAll(
                () -> assertEquals(CASE_ID.toString(), caseDetails.getId()),
                () -> assertEquals(CASE_REFERENCE, caseDetails.getReference())
            );

            Set<String> roles = rolesCaptor.getValue();
            assertAll(
                () -> assertEquals(1, roles.size()),
                () -> assertTrue(roles.contains(CASE_ROLE))
            );
        }

        @Test
        @DisplayName("should not add case user role when same role with different case exists")
        void shouldNotAddCaseUserRoleWhenRoleIsCaseInsensitive() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            String role = "[DEFENDANT]";
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, role)
            );
            // register existing case role
            mockExistingCaseUserRoles(List.of(
                createCaseUserEntity(CASE_ID, "[defendant]", USER_ID))
            );

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(caseUserRepository, never()).grantAccess(CASE_ID, USER_ID, role);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should not add case user role when same role with different case exists")
        void shouldNotAddCaseUserRoleWhenRoleIsCaseInsensitiveForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            String role = "[DEFENDANT]";
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, role)
            );
            // register existing case role
            mockExistingCaseUserRolesForRA(List.of(
                new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, "[defendant]")
            ));

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(roleAssignmentService, never()).createCaseRoleAssignments(
                any(CaseDetails.class),
                eq(USER_ID),
                any(),
                anyBoolean()
            );

            verifyNoInteractions(caseUserRepository);
        }

        @Test
        @DisplayName("should add multiple case user roles")
        void shouldAddMultipleCaseUserRoles() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            final CaseDetails caseDetailsOther = new CaseDetails();
            caseDetailsOther.setId(String.valueOf(CASE_ID_OTHER));
            caseDetailsOther.setReference(CASE_REFERENCE_OTHER);
            doReturn(Optional.of(caseDetailsOther)).when(caseDetailsRepository).findByReference(null,
                CASE_REFERENCE_OTHER);
            // behave as no existing case roles
            mockExistingCaseUserRoles(new ArrayList<>());

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE)
            );

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID, USER_ID, CASE_ROLE);
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID_OTHER, USER_ID, CASE_ROLE);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should add multiple case user roles")
        void shouldAddMultipleCaseUserRolesForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            final CaseDetails caseDetailsOther = new CaseDetails();
            caseDetailsOther.setId(String.valueOf(CASE_ID_OTHER));
            caseDetailsOther.setReference(CASE_REFERENCE_OTHER);
            doReturn(Optional.of(caseDetailsOther)).when(caseDetailsRepository).findByReference(null,
                CASE_REFERENCE_OTHER);
            // behave as no existing case roles
            mockExistingCaseUserRolesForRA(new ArrayList<>());

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE)
            );

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(roleAssignmentService, times(2)).createCaseRoleAssignments(
                caseDetailsCaptor.capture(),
                eq(USER_ID),
                rolesCaptor.capture(),
                eq(false)
            );
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID, USER_ID, CASE_ROLE);
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID_OTHER, USER_ID, CASE_ROLE);

            // :: create map of captor value indexes
            List<CaseDetails> caseDetailsList = caseDetailsCaptor.getAllValues();
            List<Set<String>> rolesList = rolesCaptor.getAllValues();
            assertAll(
                () -> assertEquals(2, caseDetailsList.size()),
                () -> assertEquals(2, rolesList.size())
            );
            Map<Long, Integer> captorValueMap = new HashMap<>();
            captorValueMap.put(caseDetailsList.get(0).getReference(), 0);
            captorValueMap.put(caseDetailsList.get(1).getReference(), 1);

            // :: load captor details for CASE_REFERENCE
            Integer index1 = captorValueMap.get(CASE_REFERENCE);
            CaseDetails caseDetails1 = caseDetailsList.get(index1);
            Set<String> roles1 = rolesList.get(index1);

            assertAll(
                () -> assertEquals(CASE_ID.toString(), caseDetails1.getId()),
                () -> assertEquals(CASE_REFERENCE, caseDetails1.getReference())
            );
            assertAll(
                () -> assertEquals(1, roles1.size()),
                () -> assertTrue(roles1.contains(CASE_ROLE))
            );

            // :: load captor details for CASE_REFERENCE_OTHER
            Integer index2 = captorValueMap.get(CASE_REFERENCE_OTHER);
            CaseDetails caseDetails2 = caseDetailsList.get(index2);
            Set<String> roles2 = rolesList.get(index2);

            assertAll(
                () -> assertEquals(CASE_ID_OTHER.toString(), caseDetails2.getId()),
                () -> assertEquals(CASE_REFERENCE_OTHER, caseDetails2.getReference())
            );
            assertAll(
                () -> assertEquals(1, roles2.size()),
                () -> assertTrue(roles2.contains(CASE_ROLE))
            );
        }

        @Test
        @DisplayName("should add multiple case user roles but lookup case details once per case")
        void shouldAddMultipleCaseUserRolesButLoadCaseDetailsOncePerCase() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                // NB: repeat case reference
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER)
            );
            // behave as no existing case roles
            mockExistingCaseUserRoles(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            // NB: only one lookup per case reference
            verify(caseDetailsRepository, times(1)).findByReference(null, CASE_REFERENCE);
            // standard grant access check for all case user roles
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID, USER_ID, CASE_ROLE);
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID, USER_ID, CASE_ROLE_OTHER);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should add multiple case user roles but lookup case details once per case")
        void shouldAddMultipleCaseUserRolesButLoadCaseDetailsOncePerCaseForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                // NB: repeat case reference
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER)
            );
            // behave as no existing case roles
            mockExistingCaseUserRolesForRA(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            // NB: only one lookup per case reference
            verify(caseDetailsRepository, times(1)).findByReference(null, CASE_REFERENCE);
            // single submission of all roles for case
            verify(roleAssignmentService).createCaseRoleAssignments(
                caseDetailsCaptor.capture(),
                eq(USER_ID),
                rolesCaptor.capture(),
                eq(false)
            );
            // standard grant access check for all case user roles
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID, USER_ID, CASE_ROLE);
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID, USER_ID, CASE_ROLE_OTHER);

            CaseDetails caseDetails = caseDetailsCaptor.getValue();
            assertAll(
                () -> assertEquals(CASE_ID.toString(), caseDetails.getId()),
                () -> assertEquals(CASE_REFERENCE, caseDetails.getReference())
            );

            Set<String> roles = rolesCaptor.getValue();
            assertAll(
                () -> assertEquals(2, roles.size()),
                () -> assertTrue(roles.contains(CASE_ROLE)),
                () -> assertTrue(roles.contains(CASE_ROLE_OTHER))
            );
        }

        @Test
        @DisplayName("should increment organisation user count for single new case-user relationship")
        void shouldIncrementOrganisationUserCountForSingleNewRelationship() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );
            // behave as no existing case roles
            mockExistingCaseUserRoles(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), 1L);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should increment organisation user count for single new case-user relationship")
        void shouldIncrementOrganisationUserCountForSingleNewRelationshipForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );
            // behave as no existing case roles
            mockExistingCaseUserRolesForRA(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), 1L);
        }

        @Test
        @DisplayName("should not increment organisation user count for single new case-user with [CREATOR] role")
        void shouldNotIncrementOrganisationUserCountForSingleNewRelationshipWithCreatorRole() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION
                )
            );
            // behave as no existing case roles
            mockExistingCaseUserRoles(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName(
            "RA set to true, should not increment organisation user count for single new case-user with [CREATOR] role"
        )
        void shouldNotIncrementOrganisationUserCountForSingleNewRelationshipWithCreatorRoleForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION
                )
            );
            // behave as no existing case roles
            mockExistingCaseUserRolesForRA(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should not increment organisation user count for existing case-user relationship")
        void shouldNotIncrementOrganisationUserCountForExistingRelationship() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );
            // register existing case role
            mockExistingCaseUserRoles(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID)
            ));

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should not increment organisation user count for existing case-user relationship")
        void shouldNotIncrementOrganisationUserCountForExistingRelationshipForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );
            // register existing case role
            mockExistingCaseUserRolesForRA(List.of(
                new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
            ));

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());

            verifyNoInteractions(caseUserRepository);
        }

        @Test
        @DisplayName("should increment organisation user count only once for repeat new case-user relationship")
        void shouldIncrementOrganisationUserCountOnlyOnceForRepeatNewRelationship() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER,
                    ORGANISATION)
            );
            // behave as no existing case roles
            mockExistingCaseUserRoles(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), 1L);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName(
            "RA set to true, should increment organisation user count only once for repeat new case-user relationship"
        )
        void shouldIncrementOrganisationUserCountOnlyOnceForRepeatNewRelationshipForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER,
                    ORGANISATION)
            );
            // behave as no existing case roles
            mockExistingCaseUserRolesForRA(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), 1L);
        }

        @Test
        @DisplayName("should increment organisation user count for new case-user relationship without [CREATOR] role")
        void shouldIncrementOrganisationUserCountForNewRelationshipsThatDoNotContainCreatorRole() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );
            // behave as no existing case roles
            mockExistingCaseUserRoles(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION),1L);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName(
            "RA set to true, should increment organisation user count for new case-user relationship without "
                + "[CREATOR] role"
        )
        void shouldIncrementOrganisationUserCountForNewRelationshipsThatDoNotContainCreatorRoleForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );
            // behave as no existing case roles
            mockExistingCaseUserRolesForRA(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION),1L);
        }

        @Test
        @DisplayName("should increment organisation user count for new case-user relationship with existing [CREATOR]")
        void shouldIncrementOrganisationUserCountForNewRelationshipsWithExistingCreatorRole() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );
            // register existing [CREATOR] case role
            mockExistingCaseUserRoles(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)
            ));

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION),1L);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName(
            "RA set to true, should increment organisation user count for new case-user relationship with"
                + " existing [CREATOR]"
        )
        void shouldIncrementOrganisationUserCountForNewRelationshipsWithExistingCreatorRoleForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );
            // register existing [CREATOR] case role
            mockExistingCaseUserRolesForRA(List.of(
                new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
            ));

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION),1L);
        }

        @Test
        @DisplayName("should not increment organisation user count for new [CREATOR] with existing relationship")
        void shouldNotIncrementOrganisationUserCountForNewCreatorRoleWithExistingRelationships() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );
            // register existing case role
            mockExistingCaseUserRoles(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID)
            ));

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName(
            "RA set to true, should not increment organisation user count for new [CREATOR] with existing relationship"
        )
        void shouldNotIncrementOrganisationUserCountForNewCreatorRoleWithExistingRelationshipsForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );
            // register existing case role
            mockExistingCaseUserRolesForRA(List.of(
                new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
            ));

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should increment organisation user count for multiple new case-user relationship")
        void shouldIncrementOrganisationUserCountForMultipleNewRelationships() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                // CASE_REFERENCE/CASE_ID
                // (2 orgs with 2 users with 2 roles >> 2 org counts incremented by 2)
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE,
                    ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER,
                    ORGANISATION_OTHER),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE_OTHER,
                    ORGANISATION_OTHER),

                // CASE_REFERENCE_OTHER/CASE_ID_OTHER
                // (2 orgs with 1 user each with multiple roles >> 2 org counts incremented by 1)
                // (however 2nd org count will not be required as existing relationship added below **)
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE,
                    ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE_OTHER,
                    ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID_OTHER, CASE_ROLE,
                    ORGANISATION_OTHER),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID_OTHER,
                    CASE_ROLE_OTHER, ORGANISATION_OTHER)

            );
            // register existing case role
            mockExistingCaseUserRoles(List.of(
                // ** CASE_REFERENCE_OTHER + USER_ID_OTHER as exiting relationship
                // (i.e. to check adjusting count still works in multiple)
                createCaseUserEntity(CASE_ID_OTHER, CASE_ROLE_OTHER, USER_ID_OTHER)
            ));

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            // verify CASE_REFERENCE/CASE_ID
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), 2L);
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION_OTHER),
                    2L);

            // verify CASE_REFERENCE_OTHER/CASE_ID_OTHER (NB: only 1 user per org: 2nd org has no new relationships)
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE_OTHER.toString(), getOrgUserCountSupDataKey(ORGANISATION),
                    1L);
            verify(supplementaryDataRepository, never()) // NB: never called as exiting relationship ignored
                .incrementSupplementaryData(
                    eq(CASE_REFERENCE_OTHER.toString()),
                    eq(getOrgUserCountSupDataKey(ORGANISATION_OTHER)),
                    anyLong()
                );

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should increment organisation user count for multiple new case-user relationship")
        void shouldIncrementOrganisationUserCountForMultipleNewRelationshipsForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                // CASE_REFERENCE/CASE_ID
                // (2 orgs with 2 users with 2 roles >> 2 org counts incremented by 2)
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE,
                    ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER,
                    ORGANISATION_OTHER),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE_OTHER,
                    ORGANISATION_OTHER),

                // CASE_REFERENCE_OTHER/CASE_ID_OTHER
                // (2 orgs with 1 user each with multiple roles >> 2 org counts incremented by 1)
                // (however 2nd org count will not be required as existing relationship added below **)
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE,
                    ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE_OTHER,
                    ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID_OTHER, CASE_ROLE,
                    ORGANISATION_OTHER),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID_OTHER,
                    CASE_ROLE_OTHER, ORGANISATION_OTHER)

            );
            // register existing case role
            mockExistingCaseUserRolesForRA(List.of(
                // ** CASE_REFERENCE_OTHER + USER_ID_OTHER as exiting relationship
                // (i.e. to check adjusting count still works in multiple)
                new CaseAssignedUserRole(CASE_REFERENCE_OTHER.toString(), USER_ID_OTHER, CASE_ROLE_OTHER)
            ));

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            // verify CASE_REFERENCE/CASE_ID
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), 2L);
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION_OTHER),
                    2L);

            // verify CASE_REFERENCE_OTHER/CASE_ID_OTHER (NB: only 1 user per org: 2nd org has no new relationships)
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE_OTHER.toString(), getOrgUserCountSupDataKey(ORGANISATION),
                    1L);
            verify(supplementaryDataRepository, never()) // NB: never called as exiting relationship ignored
                .incrementSupplementaryData(
                    eq(CASE_REFERENCE_OTHER.toString()),
                    eq(getOrgUserCountSupDataKey(ORGANISATION_OTHER)),
                    anyLong()
                );
        }

    }


    @Nested()
    @DisplayName("removeCaseUserRoles(caseUserRoles)")
    class RemoveCaseAssignUserRoles {

        @Captor
        private ArgumentCaptor<List<RoleAssignmentsDeleteRequest>> deleteRequestsCaptor;

        @BeforeEach
        void setUp() {
            configureCaseRepository(null);
        }

        @Test
        @DisplayName("should remove single case user role")
        void shouldRemoveSingleCaseUserRole() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRoles(
                // before
                List.of(createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(caseUserRepository, times(1)).revokeAccess(CASE_ID, USER_ID, CASE_ROLE);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should remove single case user role")
        void shouldRemoveSingleCaseUserRoleForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture());
            verify(caseUserRepository, times(1)).revokeAccess(CASE_ID, USER_ID, CASE_ROLE);

            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(1, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE),
                    deleteRequests.get(0)
                )
            );
        }

        @Test
        @DisplayName("RA set to true, db sync false, should remove single case user role")
        void shouldRemoveSingleCaseUserRoleForRAWithDbSyncFalse() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);
            when(applicationParams.getEnableCaseUsersDbSync()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture());
            verifyNoInteractions(caseUserRepository);

            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(1, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE),
                    deleteRequests.get(0)
                )
            );
        }

        @Test
        @DisplayName("should remove single [CREATOR] case user role")
        void shouldRemoveSingleCreatorCaseUserRole() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRoles(
                // before
                List.of(createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(caseUserRepository, times(1)).revokeAccess(CASE_ID, USER_ID, CASE_ROLE_CREATOR);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should remove single [CREATOR] case user role")
        void shouldRemoveSingleCreatorCaseUserRoleForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture());
            verify(caseUserRepository, times(1)).revokeAccess(CASE_ID, USER_ID, CASE_ROLE_CREATOR);

            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(1, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE_CREATOR),
                    deleteRequests.get(0)
                )
            );
        }

        @Test
        @DisplayName("should remove multiple case user roles")
        void shouldRemoveMultipleCaseUserRoles() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            final CaseDetails caseDetailsOther = new CaseDetails();
            caseDetailsOther.setId(String.valueOf(CASE_ID_OTHER));
            caseDetailsOther.setReference(CASE_REFERENCE_OTHER);
            doReturn(Optional.of(caseDetailsOther)).when(caseDetailsRepository).findByReference(null,
                CASE_REFERENCE_OTHER);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRoles(
                // before
                List.of(
                    createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID),
                    createCaseUserEntity(CASE_ID_OTHER, CASE_ROLE, USER_ID)
                ),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(caseUserRepository, times(1)).revokeAccess(CASE_ID, USER_ID, CASE_ROLE);
            verify(caseUserRepository, times(1))
                .revokeAccess(CASE_ID_OTHER, USER_ID, CASE_ROLE);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should remove multiple case user roles")
        void shouldRemoveMultipleCaseUserRolesForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            final CaseDetails caseDetailsOther = new CaseDetails();
            caseDetailsOther.setId(String.valueOf(CASE_ID_OTHER));
            caseDetailsOther.setReference(CASE_REFERENCE_OTHER);
            doReturn(Optional.of(caseDetailsOther)).when(caseDetailsRepository).findByReference(null,
                CASE_REFERENCE_OTHER);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE)
                ),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture());
            verify(caseUserRepository, times(1)).revokeAccess(CASE_ID, USER_ID, CASE_ROLE);
            verify(caseUserRepository, times(1)).revokeAccess(CASE_ID_OTHER, USER_ID, CASE_ROLE);

            Map<String, RoleAssignmentsDeleteRequest> deleteRequestsMapByCaseId =
                deleteRequestsCaptor.getValue().stream()
                    .collect(Collectors.toMap(
                        RoleAssignmentsDeleteRequest::getCaseId, deleteRequests -> deleteRequests)
                    );

            assertAll(
                () -> assertEquals(2, deleteRequestsMapByCaseId.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE),
                    deleteRequestsMapByCaseId.get(CASE_REFERENCE.toString())
                ),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE_OTHER.toString(), USER_ID, List.of(CASE_ROLE),
                    deleteRequestsMapByCaseId.get(CASE_REFERENCE_OTHER.toString())
                )
            );
        }

        @Test
        @DisplayName("should throw not found exception when case not found")
        void shouldThrowNotFound() {

            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_NOT_FOUND.toString(), USER_ID, CASE_ROLE)
            );

            // ACT / ASSERT
            assertThrows(CaseNotFoundException.class, () -> caseAccessOperation.removeCaseUserRoles(caseUserRoles));

            verifyNoInteractions(caseUserRepository);
            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("should decrement organisation user count for single new case-user relationship")
        void shouldDecrementOrganisationUserCountForSingleNewRelationship() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRoles(
                // before
                List.of(createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should decrement organisation user count for single new case-user relationship")
        void shouldDecrementOrganisationUserCountForSingleNewRelationshipForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);
        }

        @Test
        @DisplayName("should not decrement organisation user count for non-existing case-user relationship")
        void shouldNotDecrementOrganisationUserCountForExistingRelationship() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // no existing relationship
            mockExistingCaseUserRoles(new ArrayList<>());

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName(
            "RA set to true, should not decrement organisation user count for non-existing case-user relationship"
        )
        void shouldNotDecrementOrganisationUserCountForExistingRelationshipForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // no existing relationship
            mockExistingCaseUserRolesForRA(new ArrayList<>());

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());

            verifyNoInteractions(caseUserRepository);
        }

        @Test
        @DisplayName("should not decrement organisation user count for single new case-user with [CREATOR] role")
        void shouldNotIncrementOrganisationUserCountWithCreatorRole() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRoles(
                // before
                List.of(createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName(
            "RA set to true, should not decrement organisation user count for single new case-user with [CREATOR] role"
        )
        void shouldNotIncrementOrganisationUserCountWithCreatorRoleForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should decrement organisation user count only once for repeat remove case-user relationship")
        void shouldDecrementOrganisationUserCountOnlyOnceForRepeatRelationship() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRoles(
                // before
                List.of(createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName(
            "RA set to true, "
                + "should decrement organisation user count only once for repeat remove case-user relationship"
        )
        void shouldDecrementOrganisationUserCountOnlyOnceForRepeatRelationshipForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);
        }

        @Test
        @DisplayName("should decrement organisation user count only once and ignore creator role")
        void shouldDecrementOrganisationUserCountOnlyOnceForRepeatRelationshipIgnoreCreatorRole() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRoles(
                // before
                List.of(
                    createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID),
                    createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)
                ),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should decrement organisation user count only once and ignore creator role")
        void shouldDecrementOrganisationUserCountOnlyOnceForRepeatRelationshipIgnoreCreatorRoleForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
                ),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);
        }

        @Test
        @DisplayName("should decrement organisation user count for single creator role after removing other roles")
        void shouldDecrementOrganisationUserCountForSingleCreatorRoleAfterRemovingOtherRoles() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRoles(
                // before
                List.of(
                    createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID),
                    createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)
                ),
                // after
                List.of(
                    createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)
                )
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName(
            "RA set to true, "
                + "should decrement organisation user count for single creator role after removing other roles"
        )
        void shouldDecrementOrganisationUserCountForSingleCreatorRoleAfterRemovingOtherRolesForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
                ),
                // after
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
                )
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);
        }

        @Test
        @DisplayName("should not decrement organisation user count after removing creator role")
        void shouldNotDecrementOrganisationUserCountAfterRemovingCreatorRole() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRoles(
                // before
                List.of(
                    createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID),
                    createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)
                ),
                // after
                List.of(
                    createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID)
                )
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should not decrement organisation user count after removing creator role")
        void shouldNotDecrementOrganisationUserCountAfterRemovingCreatorRoleForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
                ),
                // after
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
                )
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should not decrement organisation user count for an existing relationship with another role")
        void shouldNotDecrementOrganisationUserCountForAnExistingRelationshipWithOtherRole() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRoles(
                // before
                List.of(
                    createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID),
                    createCaseUserEntity(CASE_ID, CASE_ROLE_OTHER, USER_ID)
                ),
                // after
                List.of(
                    createCaseUserEntity(CASE_ID, CASE_ROLE_OTHER, USER_ID)
                )
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName(
            "RA set to true, "
                + "should not decrement organisation user count for an existing relationship with another role"
        )
        void shouldNotDecrementOrganisationUserCountForAnExistingRelationshipWithOtherRoleForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER)
                ),
                // after
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER)
                )
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should decrement organisation user count for multiple case-user relationship")
        void shouldDecrementOrganisationUserCountForMultipleRelationships() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                // CASE_REFERENCE/CASE_ID
                // (2 orgs with 2 users with 2 roles >> 2 org counts decremented by 1)
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE,
                    ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER,
                    ORGANISATION_OTHER)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRoles(
                // before
                List.of(
                    createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID),
                    createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID_OTHER),
                    createCaseUserEntity(CASE_ID, CASE_ROLE_OTHER, USER_ID),
                    createCaseUserEntity(CASE_ID, CASE_ROLE_OTHER, USER_ID_OTHER)
                ),
                // after
                List.of(
                    createCaseUserEntity(CASE_ID, CASE_ROLE_OTHER, USER_ID_OTHER)
                )
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            // verify CASE_REFERENCE/CASE_ID
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION_OTHER),
                    -1L);

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should decrement organisation user count for multiple case-user relationship")
        void shouldDecrementOrganisationUserCountForMultipleRelationshipsForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                // CASE_REFERENCE/CASE_ID
                // (2 orgs with 2 users with 2 roles >> 2 org counts decremented by 1)
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE,
                    ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER,
                    ORGANISATION_OTHER)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE_OTHER)
                ),
                // after
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE_OTHER)
                )
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            // verify CASE_REFERENCE/CASE_ID
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION_OTHER),
                    -1L);
        }

    }


    @Nested()
    @DisplayName("findCaseUserRoles(caseReferences, userIds)")
    class GetCaseAssignUserRoles {

        @BeforeEach
        void setUp() {
            configureCaseRepository(null);
        }

        @Test
        @DisplayName("should find case assigned user roles")
        void shouldGetCaseAssignedUserRoles() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            // register existing case role
            mockExistingCaseUserRoles(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID),
                createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)
            ));

            List<Long> caseReferences = Lists.newArrayList(CASE_REFERENCE);
            final List<String> userIds = Lists.newArrayList();

            // ACT
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(caseReferences,
                userIds);

            // ASSERT
            assertNotNull(caseAssignedUserRoles);
            assertEquals(2, caseAssignedUserRoles.size());
            assertEquals(CASE_ROLE, caseAssignedUserRoles.get(0).getCaseRole());
            assertEquals(CASE_ROLE_CREATOR, caseAssignedUserRoles.get(1).getCaseRole());
        }

        @Test
        @DisplayName("RA set to true, should find case assigned user roles")
        void shouldGetCaseAssignedUserRolesForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            // register existing case role
            mockExistingCaseUserRolesForRA(List.of(
                new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
            ));

            final var caseReferences = Lists.newArrayList(CASE_REFERENCE);
            final List<String> userIds = Lists.newArrayList();

            // ACT
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(caseReferences,
                userIds);

            // ASSERT
            assertNotNull(caseAssignedUserRoles);
            assertEquals(2, caseAssignedUserRoles.size());
            assertEquals(CASE_ROLE, caseAssignedUserRoles.get(0).getCaseRole());
            assertEquals(CASE_ROLE_CREATOR, caseAssignedUserRoles.get(1).getCaseRole());
        }

        @Test
        @DisplayName("should return empty result for non existing cases")
        void shouldReturnEmptyResultOnNonExistingCases() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);
            List<Long> caseReferences = Lists.newArrayList(CASE_NOT_FOUND);

            // ACT
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(caseReferences,
                Lists.newArrayList());

            // ASSERT
            assertNotNull(caseAssignedUserRoles);
            assertEquals(0, caseAssignedUserRoles.size());
        }

        @Test
        @DisplayName("RA set to true, should return empty result for non existing cases")
        void shouldReturnEmptyResultOnNonExistingCasesForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            List<Long> caseReferences = Lists.newArrayList(CASE_NOT_FOUND);

            // ACT
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(caseReferences,
                Lists.newArrayList());

            // ASSERT
            assertNotNull(caseAssignedUserRoles);
            assertEquals(0, caseAssignedUserRoles.size());
        }

    }


    private void configureCaseRepository(String jurisdiction) {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(String.valueOf(CASE_ID));
        caseDetails.setReference(CASE_REFERENCE);
        final CaseDetails caseDetailsOther = new CaseDetails();
        caseDetailsOther.setId(String.valueOf(CASE_ID_OTHER));
        caseDetailsOther.setReference(CASE_REFERENCE_OTHER);

        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository)
            .findByReference(jurisdiction, CASE_REFERENCE);
        doReturn(Optional.of(caseDetailsOther)).when(caseDetailsRepository)
            .findByReference(jurisdiction, CASE_REFERENCE_OTHER);
        doReturn(Optional.empty()).when(caseDetailsRepository)
            .findByReference(jurisdiction, CASE_NOT_FOUND);
        doReturn(Optional.empty()).when(caseDetailsRepository)
            .findByReference(WRONG_JURISDICTION, CASE_REFERENCE);
    }

    private CaseUserEntity createCaseUserEntity(Long caseDataId, String caseRole, String userId) {
        CaseUserEntity.CasePrimaryKey primaryKey = new CaseUserEntity.CasePrimaryKey();
        primaryKey.setCaseDataId(caseDataId);
        primaryKey.setCaseRole(caseRole);
        primaryKey.setUserId(userId);

        CaseUserEntity caseUserEntity = new CaseUserEntity();
        caseUserEntity.setCasePrimaryKey(primaryKey);

        return caseUserEntity;
    }

    private CaseUser caseUser(String...caseRoles) {
        final CaseUser caseUser = new CaseUser();
        caseUser.setUserId(USER_ID);
        caseUser.getCaseRoles().addAll(Arrays.asList(caseRoles));
        return caseUser;
    }

    private String getOrgUserCountSupDataKey(String organisationId) {
        return "orgs_assigned_users." + organisationId;
    }

    @SuppressWarnings("SameParameterValue")
    private void assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
        final String expectedCaseId,
        final String expectedUserId,
        final List<String> expectedRoleNames,
        final RoleAssignmentsDeleteRequest actualRoleAssignmentsDeleteRequest
    ) {
        assertNotNull(actualRoleAssignmentsDeleteRequest);
        assertAll(
            () -> assertEquals(expectedCaseId, actualRoleAssignmentsDeleteRequest.getCaseId()),
            () -> assertEquals(expectedUserId, actualRoleAssignmentsDeleteRequest.getUserId()),
            () -> assertEquals(expectedRoleNames.size(), actualRoleAssignmentsDeleteRequest.getRoleNames().size()),
            () -> assertThat(
                actualRoleAssignmentsDeleteRequest.getRoleNames(),
                containsInAnyOrder(expectedRoleNames.toArray())
            )
        );
    }

    private OngoingStubbing<List<CaseUserEntity>> mockExistingCaseUserRoles(
        List<CaseUserEntity> existingCaseUserRoles
    ) {
        return when(caseUserRepository.findCaseUserRoles(
            argThat(arg -> arg.contains(CASE_ID) || arg.contains(CASE_ID_OTHER)),
            argThat(arg -> arg.contains(USER_ID) || arg.isEmpty()))
        ).thenReturn(existingCaseUserRoles);
    }

    private void mockExistingCaseUserRoles(List<CaseUserEntity> existingCaseUserRoles,
                                           List<CaseUserEntity> secondCallCaseUserRoles) {
        mockExistingCaseUserRoles(existingCaseUserRoles)
            .thenReturn(secondCallCaseUserRoles);
    }

    private OngoingStubbing<List<CaseAssignedUserRole>> mockExistingCaseUserRolesForRA(
        List<CaseAssignedUserRole> existingCaseUserRoles
    ) {
        return when(roleAssignmentService.findRoleAssignmentsByCasesAndUsers(
            argThat(arg -> arg.contains(CASE_REFERENCE.toString())
                || arg.contains(CASE_REFERENCE_OTHER.toString())),
            argThat(arg -> arg.contains(USER_ID) || arg.isEmpty()))
        ).thenReturn(existingCaseUserRoles);
    }

    private void mockExistingCaseUserRolesForRA(List<CaseAssignedUserRole> existingCaseUserRoles,
                                                List<CaseAssignedUserRole> secondCallCaseUserRoles) {
        mockExistingCaseUserRolesForRA(existingCaseUserRoles)
            .thenReturn(secondCallCaseUserRoles);
    }

}
