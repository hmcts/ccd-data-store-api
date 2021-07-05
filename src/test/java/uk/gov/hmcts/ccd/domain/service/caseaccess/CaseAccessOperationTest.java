package uk.gov.hmcts.ccd.domain.service.caseaccess;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleWithOrganisation;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

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

    @Mock
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

    @InjectMocks
    private uk.gov.hmcts.ccd.domain.service.caseaccess.CaseAccessOperation caseAccessOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        configureCaseRepository(JURISDICTION);
        configureCaseRoleRepository();
        configureCaseUserRepository();
    }


    @Nested
    @DisplayName("grantAccess()")
    class GrantAccess {

        @Captor
        ArgumentCaptor<CaseDetails> caseDetailsCaptor;

        @Captor
        ArgumentCaptor<Set<String>> rolesCaptor;

        @BeforeEach
        void setUp() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        @DisplayName("should grant access to user")
        void shouldGrantAccess() {

            // GIVEN
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            // WHEN
            caseAccessOperation.grantAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            // THEN
            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CREATOR.getRole()),
                () -> verifyZeroInteractions(roleAssignmentService)
            );
        }

        @Test
        @DisplayName("RA set to true, should grant access to user")
        void shouldGrantAccessForRA() {

            // GIVEN
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            // WHEN
            caseAccessOperation.grantAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            // THEN
            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> {
                    verify(roleAssignmentService).createCaseRoleAssignments(
                        caseDetailsCaptor.capture(),
                        eq(USER_ID),
                        rolesCaptor.capture(),
                        eq(true)
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
                () -> verifyZeroInteractions(caseUserRepository)
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
                () -> verifyZeroInteractions(caseUserRepository),
                () -> verifyZeroInteractions(roleAssignmentService)
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
                () -> verifyZeroInteractions(caseUserRepository),
                () -> verifyZeroInteractions(roleAssignmentService)
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
            MockitoAnnotations.initMocks(this);

            caseDetails = new CaseDetails();
            caseDetails.setId(CASE_ID.toString());
            caseDetails.setCaseTypeId(CASE_TYPE_ID);
        }

        @Test
        @DisplayName("should reject update when it contains an unknown case role")
        void shouldRejectWhenUnknownCaseRoles() {
            final Executable execAccessUpdate = () -> caseAccessOperation.updateUserAccess(caseDetails,
                                                                                           caseUser(NOT_CASE_ROLE));
            assertThrows(InvalidCaseRoleException.class, execAccessUpdate);
            verifyZeroInteractions(caseUserRepository);
            verifyZeroInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("should grant access when added case role valid")
        void shouldGrantAccessForCaseRole() {

            // GIVEN
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            // WHEN
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

            // THEN
            verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CASE_ROLE);
        }

        @Test
        @DisplayName("RA set to true, should grant access when added case role valid")
        void shouldGrantAccessForCaseRoleForRA() {

            // GIVEN
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            // WHEN
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

            // THEN
            verify(roleAssignmentService).createCaseRoleAssignments(
                eq(caseDetails),
                eq(USER_ID),
                rolesCaptor.capture(),
                eq(true)
            );

            // :: verify roles submitted
            Set<String> roles = rolesCaptor.getValue();
            assertAll(
                () -> assertEquals(1, roles.size()),
                () -> assertTrue(roles.contains(CASE_ROLE))
            );

            // :: verify legacy service not called
            verifyZeroInteractions(caseUserRepository);
        }

        @Test
        @DisplayName("should grant access when added case roles contains global [CREATOR]")
        void shouldGrantAccessForCaseRoleCreator() {

            // GIVEN
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            // WHEN
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE, CREATOR.getRole()));

            // THEN
            assertAll(
                () -> verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CASE_ROLE),
                () -> verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CREATOR.getRole()),
                () -> verifyZeroInteractions(roleAssignmentService)
            );
        }

        @Test
        @DisplayName("RA set to true, should grant access when added case roles contains global [CREATOR]")
        void shouldGrantAccessForCaseRoleCreatorForRA() {

            // GIVEN
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

            // WHEN
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE, CREATOR.getRole()));

            // THEN
            verify(roleAssignmentService).createCaseRoleAssignments(
                eq(caseDetails),
                eq(USER_ID),
                rolesCaptor.capture(),
                eq(true)
            );

            // :: verify roles submitted
            Set<String> roles = rolesCaptor.getValue();
            assertAll(
                () -> assertEquals(2, roles.size()),
                () -> assertTrue(roles.contains(CASE_ROLE)),
                () -> assertTrue(roles.contains(CREATOR.getRole()))
            );

            // :: verify legacy service not called
            verifyZeroInteractions(caseUserRepository);
        }

        @Test
        @DisplayName("should revoke access for removed case roles")
        void shouldRevokeRemovedCaseRoles() {
            // NB: test not valid for 'Attribute Based Access Control' as :
            //     RAS submission with `replaceExisting = true` does not require us to revokeRemoved.

            // GIVEN
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            // WHEN
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

            // THEN
            assertAll(
                () -> verify(caseUserRepository).revokeAccess(CASE_ID, USER_ID, CASE_ROLE_GRANTED),
                () -> verifyZeroInteractions(roleAssignmentService)
            );
        }

        @Test
        @DisplayName("should ignore case roles already granted")
        void shouldIgnoreGrantedCaseRoles() {
            // NB: test not valid for 'Attribute Based Access Control' as :
            //     RAS submission with `replaceExisting = true` does not require us to ignoreGranted.

            // GIVEN
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

            // WHEN
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE_GRANTED));

            // THEN
            assertAll(
                () -> verify(caseUserRepository, never()).grantAccess(CASE_ID, USER_ID, CASE_ROLE_GRANTED),
                () -> verifyZeroInteractions(roleAssignmentService)
            );
        }
    }


    @Nested
    @DisplayName("revokeAccess()")
    class RevokeAccess {

        @Test
        @DisplayName("should revoke access to user")
        void shouldRevokeAccess() {
            caseAccessOperation.revokeAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> verify(caseUserRepository).revokeAccess(CASE_ID, USER_ID, CREATOR.getRole())
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
                () -> verify(caseUserRepository, never()).revokeAccess(CASE_ID, USER_ID, CREATOR.getRole())
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
                () -> verify(caseUserRepository, never()).revokeAccess(CASE_ID, USER_ID, CREATOR.getRole())
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
            when(caseUserRepository.findCasesUserIdHasAccessTo(USER_ID))
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
            MockitoAnnotations.initMocks(this);

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

            verifyZeroInteractions(caseUserRepository);
            verifyZeroInteractions(roleAssignmentService);
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

            verifyZeroInteractions(roleAssignmentService);
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

            verifyZeroInteractions(caseUserRepository);
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

            verifyZeroInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should not add case user role when same role with different case exists")
        void shouldNotAddCaseUserRoleWhenRoleIsCaseInsensitiveForRA() {

            // ARRANGE
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);

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
            verify(caseUserRepository, never()).grantAccess(CASE_ID, USER_ID, role);

            verifyZeroInteractions(roleAssignmentService);
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

            verifyZeroInteractions(roleAssignmentService);
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

            verifyZeroInteractions(caseUserRepository);
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

            verifyZeroInteractions(roleAssignmentService);
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
            mockExistingCaseUserRoles(new ArrayList<>());

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

            verifyZeroInteractions(caseUserRepository);
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

            verifyZeroInteractions(roleAssignmentService);
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

            verifyZeroInteractions(caseUserRepository);
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

            verifyZeroInteractions(roleAssignmentService);
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

            verifyZeroInteractions(caseUserRepository);
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

            verifyZeroInteractions(roleAssignmentService);
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

            verifyZeroInteractions(caseUserRepository);
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

            verifyZeroInteractions(roleAssignmentService);
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

            verifyZeroInteractions(caseUserRepository);
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

            verifyZeroInteractions(roleAssignmentService);
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

            verifyZeroInteractions(caseUserRepository);
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

            verifyZeroInteractions(roleAssignmentService);
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

            verifyZeroInteractions(caseUserRepository);
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

            verifyZeroInteractions(roleAssignmentService);
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

            verifyZeroInteractions(caseUserRepository);
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
            // ** CASE_REFERENCE_OTHER + USER_ID_OTHER as exiting relationship
            // (i.e. to check adjusting count still works in multiple)
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID) && arg.contains(CASE_ID_OTHER)),
                argThat(arg -> arg.contains(USER_ID) && arg.contains(USER_ID_OTHER))
            )).thenReturn(Collections.singletonList(
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

            verifyZeroInteractions(roleAssignmentService);
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
            // ** CASE_REFERENCE_OTHER + USER_ID_OTHER as exiting relationship
            // (i.e. to check adjusting count still works in multiple)
            when(roleAssignmentService.findRoleAssignmentsByCasesAndUsers(
                argThat(arg -> arg.contains(CASE_REFERENCE.toString())
                            && arg.contains(CASE_REFERENCE_OTHER.toString())),
                argThat(arg -> arg.contains(USER_ID) && arg.contains(USER_ID_OTHER))
            )).thenReturn(Collections.singletonList(
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

            verifyZeroInteractions(caseUserRepository);
        }

        private void mockExistingCaseUserRoles(List<CaseUserEntity> existingCaseUserRoles) {
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID) || arg.contains(CASE_ID_OTHER)),
                argThat(arg -> arg.contains(USER_ID)))
            ).thenReturn(existingCaseUserRoles);
        }

        private void mockExistingCaseUserRolesForRA(List<CaseAssignedUserRole> existingCaseUserRoles) {
            when(roleAssignmentService.findRoleAssignmentsByCasesAndUsers(
                argThat(arg -> arg.contains(CASE_REFERENCE.toString())
                            || arg.contains(CASE_REFERENCE_OTHER.toString())),
                argThat(arg -> arg.contains(USER_ID)))
            ).thenReturn(existingCaseUserRoles);
        }

    }


    @Nested()
    @DisplayName("removeCaseUserRoles(caseUserRoles)")
    class RemoveCaseAssignUserRoles {

        @BeforeEach
        void setUp() {
            configureCaseRepository(null);
        }

        @Test
        @DisplayName("should remove single case user role")
        void shouldRemoveSingleCaseUserRole() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
            );

            // for an existing relation and then after removal
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID)
            )).thenReturn(new ArrayList<>());

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(caseUserRepository, times(1)).revokeAccess(CASE_ID, USER_ID, CASE_ROLE);
        }

        @Test
        @DisplayName("should remove single [CREATOR] case user role")
        void shouldRemoveSingleCreatorCaseUserRole() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
            );

            // for an existing relation and then after removal
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)
            )).thenReturn(new ArrayList<>());

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(caseUserRepository, times(1)).revokeAccess(CASE_ID, USER_ID, CASE_ROLE_CREATOR);
        }

        @Test
        @DisplayName("should remove multiple case user roles")
        void shouldRemoveMultipleCaseUserRoles() {
            // ARRANGE
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
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID) && arg.contains(CASE_ID_OTHER)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(List.of(createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID),
                createCaseUserEntity(CASE_ID_OTHER, CASE_ROLE, USER_ID)));

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(caseUserRepository, times(1)).revokeAccess(CASE_ID, USER_ID, CASE_ROLE);
            verify(caseUserRepository, times(1))
                .revokeAccess(CASE_ID_OTHER, USER_ID, CASE_ROLE);
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
        }

        @Test
        @DisplayName("should decrement organisation user count for single new case-user relationship")
        void shouldDecrementOrganisationUserCountForSingleNewRelationship() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );
            // for an existing relation and then after removal
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(List.of(createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID)))
            .thenReturn(new ArrayList<>());

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
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );
            // no existing relationship
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(new ArrayList<>());

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should not decrement organisation user count for single new case-user with [CREATOR] role")
        void shouldNotIncrementOrganisationUserCountWithCreatorRole() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );

            // for an existing relation and then after removal
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(
                List.of(createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)
            )).thenReturn(new ArrayList<>());

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should decrement organisation user count only once for repeat remove case-user relationship")
        void shouldDecrementOrganisationUserCountOnlyOnceForRepeatRelationship() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(List.of(createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID)))
                .thenReturn(new ArrayList<>());

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
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );

            // for an existing relation and then after removal
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID),
                createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)
            )).thenReturn(new ArrayList<>());

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
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID),
                createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)
            )).thenReturn(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)
            ));

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
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );

            // for an existing relation and then after removal
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID),
                createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID)
            )).thenReturn(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID)
            ));

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should not decrement organisation user count for an existing relationship with another role")
        void shouldNotDecrementOrganisationUserCountForAnExistingRelationshipWithOtherRole() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID),
                createCaseUserEntity(CASE_ID, CASE_ROLE_OTHER, USER_ID)
            )).thenReturn(List.of(createCaseUserEntity(CASE_ID, CASE_ROLE_OTHER, USER_ID)));

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should decrement organisation user count for multiple case-user relationship")
        void shouldDecrementOrganisationUserCountForMultipleRelationships() {
            // ARRANGE
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
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID) && arg.contains(USER_ID_OTHER))
            )).thenReturn(List.of(
                createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID), createCaseUserEntity(CASE_ID, CASE_ROLE,
                    USER_ID_OTHER),
                createCaseUserEntity(CASE_ID, CASE_ROLE_OTHER, USER_ID), createCaseUserEntity(CASE_ID, CASE_ROLE_OTHER,
                    USER_ID_OTHER)
            ))
                .thenReturn(List.of(createCaseUserEntity(CASE_ID, CASE_ROLE_OTHER, USER_ID_OTHER)));

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
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);
            List<Long> caseReferences = Lists.newArrayList(CASE_REFERENCE);
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(caseReferences,
                Lists.newArrayList());

            assertNotNull(caseAssignedUserRoles);
            assertEquals(2, caseAssignedUserRoles.size());
            assertEquals(CASE_ROLE, caseAssignedUserRoles.get(0).getCaseRole());
            assertEquals(CASE_ROLE_CREATOR, caseAssignedUserRoles.get(1).getCaseRole());
        }

        @Test
        @DisplayName("RA set to true, should find case assigned user roles")
        void shouldGetCaseAssignedUserRolesForRA() {
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);
            final var caseReferences = Lists.newArrayList(CASE_REFERENCE);
            final List<String> userIds = Lists.newArrayList();
            when(roleAssignmentService.findRoleAssignmentsByCasesAndUsers(anyList(),anyList()))
                .thenReturn(getCaseAssignedUserRoles());

            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(caseReferences,
                userIds);

            assertNotNull(caseAssignedUserRoles);
            assertEquals(1, caseAssignedUserRoles.size());
            assertEquals(CASE_ROLE, caseAssignedUserRoles.get(0).getCaseRole());
        }

        @Test
        @DisplayName("should return empty result for non existing cases")
        void shouldReturnEmptyResultOnNonExistingCases() {
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);
            List<Long> caseReferences = Lists.newArrayList(CASE_NOT_FOUND);
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(caseReferences,
                Lists.newArrayList());

            assertNotNull(caseAssignedUserRoles);
            assertEquals(0, caseAssignedUserRoles.size());
        }

        @Test
        @DisplayName("RA set to true, should return empty result for non existing cases")
        void shouldReturnEmptyResultOnNonExistingCasesForRA() {
            when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);
            List<Long> caseReferences = Lists.newArrayList(CASE_NOT_FOUND);
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(caseReferences,
                Lists.newArrayList());

            assertNotNull(caseAssignedUserRoles);
            assertEquals(0, caseAssignedUserRoles.size());
        }

        private List<CaseAssignedUserRole> getCaseAssignedUserRoles() {
            return Collections.singletonList(
                new CaseAssignedUserRole("caseDataId", "userId", CASE_ROLE)
            );
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

    private void configureCaseRoleRepository() {
        when(caseRoleRepository.getCaseRoles(CASE_TYPE_ID)).thenReturn(Sets.newHashSet(CASE_ROLE,
                                                                                       CASE_ROLE_CREATOR,
                                                                                       CASE_ROLE_OTHER,
                                                                                       CASE_ROLE_GRANTED));
    }

    private void configureCaseUserRepository() {
        when(caseUserRepository.findCaseRoles(CASE_ID,
                                              USER_ID)).thenReturn(Collections.singletonList(CASE_ROLE_GRANTED));
        CaseUserEntity caseUserEntity = createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID);
        CaseUserEntity caseUserEntity1 = createCaseUserEntity(CASE_ID, CASE_ROLE_CREATOR, USER_ID);
        when(caseUserRepository.findCaseUserRoles(anyList(), anyList()))
            .thenReturn(Arrays.asList(caseUserEntity, caseUserEntity1));
    }

    private CaseUserEntity createCaseUserEntity(Long caseDatatId, String caseRole, String userId) {
        CaseUserEntity.CasePrimaryKey primaryKey = new CaseUserEntity.CasePrimaryKey();
        primaryKey.setCaseDataId(caseDatatId);
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

}
