package uk.gov.hmcts.ccd.domain.service.caseaccess;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.InvalidCaseRoleException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseUser;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @InjectMocks
    private CaseAccessOperation caseAccessOperation;


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

        @Test
        @DisplayName("should grant access to user")
        void shouldGrantAccess() {
            caseAccessOperation.grantAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CREATOR.getRole())
            );
        }

        @Test
        @DisplayName("should throw not found exception when reference not found")
        void shouldThrowNotFound() {
            assertAll(
                () -> assertThrows(CaseNotFoundException.class, () -> {
                    caseAccessOperation.grantAccess(JURISDICTION, CASE_NOT_FOUND.toString(), USER_ID);
                }),
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_NOT_FOUND),
                () -> verify(caseUserRepository, never()).grantAccess(CASE_ID, USER_ID, CREATOR.getRole())
            );
        }

        @Test
        @DisplayName("should throw not found exception when reference in different jurisdiction")
        void shouldHandleWrongJurisdiction() {
            assertAll(
                () -> assertThrows(CaseNotFoundException.class, () -> {
                    caseAccessOperation.grantAccess(WRONG_JURISDICTION, CASE_REFERENCE.toString(), USER_ID);
                }),
                () -> verify(caseDetailsRepository).findByReference(WRONG_JURISDICTION, CASE_REFERENCE),
                () -> verify(caseUserRepository, never()).grantAccess(CASE_ID, USER_ID, CREATOR.getRole())
            );
        }
    }

    @Nested()
    @DisplayName("updateUserAccess(reference, caseUser)")
    class GrantAccessCaseUser {
        private CaseDetails caseDetails;

        @BeforeEach
        void setUp() {
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
        }

        @Test
        @DisplayName("should grant access when added case role valid")
        void shouldGrantAccessForCaseRole() {
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

            verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CASE_ROLE);
        }

        @Test
        @DisplayName("should grant access when added case roles contains global [CREATOR]")
        void shouldGrantAccessForCaseRoleCreator() {
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE, CREATOR.getRole()));

            assertAll(
                () -> verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CASE_ROLE),
                () -> verify(caseUserRepository).grantAccess(CASE_ID, USER_ID, CREATOR.getRole())
            );
        }

        @Test
        @DisplayName("should revoke access for removed case roles")
        void shouldRevokeRemovedCaseRoles() {
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

            verify(caseUserRepository).revokeAccess(CASE_ID, USER_ID, CASE_ROLE_GRANTED);
        }

        @Test
        @DisplayName("should ignore case roles already granted")
        void shouldIgnoreGrantedCaseRoles() {
            caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE_GRANTED));

            verify(caseUserRepository, never()).grantAccess(CASE_ID, USER_ID, CASE_ROLE_GRANTED);
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
            assertAll(
                () -> assertThrows(CaseNotFoundException.class, () -> {
                    caseAccessOperation.revokeAccess(JURISDICTION, CASE_NOT_FOUND.toString(), USER_ID);
                }),
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_NOT_FOUND),
                () -> verify(caseUserRepository, never()).revokeAccess(CASE_ID, USER_ID, CREATOR.getRole())
            );
        }

        @Test
        @DisplayName("should throw not found exception when reference in different jurisdiction")
        void shouldHandleWrongJurisdiction() {
            assertAll(
                () -> assertThrows(CaseNotFoundException.class, () -> {
                    caseAccessOperation.revokeAccess(WRONG_JURISDICTION, CASE_REFERENCE.toString(), USER_ID);
                }),
                () -> verify(caseDetailsRepository).findByReference(WRONG_JURISDICTION, CASE_REFERENCE),
                () -> verify(caseUserRepository, never()).revokeAccess(CASE_ID, USER_ID, CREATOR.getRole())
            );
        }
    }

    @Nested()
    @DisplayName("addCaseUserRoles(caseUserRoles)")
    class AddCaseAssignUserRoles {

        @BeforeEach
        void setUp() {
            configureCaseRepository(null);
        }

        @Test
        @DisplayName("should add single case user role")
        void shouldAddSingleCaseUserRole() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
            );

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID, USER_ID, CASE_ROLE);
        }

        @Test
        @DisplayName("should add multiple case user roles")
        void shouldAddMultipleCaseUserRoles() {
            // ARRANGE
            final CaseDetails caseDetailsOther = new CaseDetails();
            caseDetailsOther.setId(String.valueOf(CASE_ID_OTHER));
            caseDetailsOther.setReference(CASE_REFERENCE_OTHER);
            doReturn(Optional.of(caseDetailsOther)).when(caseDetailsRepository).findByReference(null, CASE_REFERENCE_OTHER);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE)
            );

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID, USER_ID, CASE_ROLE);
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID_OTHER, USER_ID, CASE_ROLE);
        }

        @Test
        @DisplayName("should add multiple case user roles but lookup case details once per case")
        void shouldAddMultipleCaseUserRolesButLoadCaseDetailsOncePerCase() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER) // NB: repeat case reference
            );

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            // NB: only one lookup per case reference
            verify(caseDetailsRepository, times(1)).findByReference(null, CASE_REFERENCE);
            // standard grant access check for all case user roles
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID, USER_ID, CASE_ROLE);
            verify(caseUserRepository, times(1)).grantAccess(CASE_ID, USER_ID, CASE_ROLE_OTHER);
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
        }

        @Test
        @DisplayName("should increment organisation user count for single new case-user relationship")
        void shouldIncrementOrganisationUserCountForSingleNewRelationship() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );
            // behave as a new relationship
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), 1L);
        }

        @Test
        @DisplayName("should not increment organisation user count for existing case-user relationship")
        void shouldNotIncrementOrganisationUserCountForExistingRelationship() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );
            // behave as a new relationship
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(Collections.singletonList(
                createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID)
            ));

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should increment organisation user count only once for repeat new case-user relationship")
        void shouldIncrementOrganisationUserCountOnlyOnceForRepeatNewRelationship() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER, ORGANISATION)
            );
            // behave as a new relationship
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID)),
                argThat(arg -> arg.contains(USER_ID))
            )).thenReturn(new ArrayList<>());

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), 1L);
        }

        @Test
        @DisplayName("should increment organisation user count for multiple new case-user relationship")
        void shouldIncrementOrganisationUserCountForMultipleNewRelationships() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                // CASE_REFERENCE/CASE_ID
                // (2 orgs with 2 users with 2 roles >> 2 org counts incremented by 2)
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER, ORGANISATION_OTHER),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE_OTHER, ORGANISATION_OTHER),

                // CASE_REFERENCE_OTHER/CASE_ID_OTHER
                // (2 orgs with 1 user each with multiple roles >> 2 org counts incremented by 1)
                // (however 2nd org count will not be required as existing relationship added below **)
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE_OTHER, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID_OTHER, CASE_ROLE, ORGANISATION_OTHER),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID_OTHER, CASE_ROLE_OTHER, ORGANISATION_OTHER)

            );
            // ** CASE_REFERENCE_OTHER + USER_ID_OTHER as exiting relationship (i.e. to check adjusting count still works in multiple)
            when(caseUserRepository.findCaseUserRoles(
                argThat(arg -> arg.contains(CASE_ID) && arg.contains(CASE_ID_OTHER)),
                argThat(arg -> arg.contains(USER_ID) && arg.contains(USER_ID_OTHER))
            )).thenReturn(Collections.singletonList(createCaseUserEntity(CASE_ID_OTHER, CASE_ROLE_OTHER, USER_ID_OTHER)));

            // ACT
            caseAccessOperation.addCaseUserRoles(caseUserRoles);

            // ASSERT
            // verify CASE_REFERENCE/CASE_ID
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), 2L);
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION_OTHER), 2L);

            // verify CASE_REFERENCE_OTHER/CASE_ID_OTHER (NB: only 1 user per org: 2nd org has no new relationships)
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE_OTHER.toString(), getOrgUserCountSupDataKey(ORGANISATION), 1L);
            verify(supplementaryDataRepository, never()) // NB: never called as exiting relationship ignored
                .incrementSupplementaryData(
                    eq(CASE_REFERENCE_OTHER.toString()),
                    eq(getOrgUserCountSupDataKey(ORGANISATION_OTHER)),
                    anyLong()
                );
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
            List<Long> caseReferences = Lists.newArrayList(CASE_REFERENCE);
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(caseReferences, Lists.newArrayList());

            assertNotNull(caseAssignedUserRoles);
            assertEquals(1, caseAssignedUserRoles.size());
            assertEquals(CASE_ROLE, caseAssignedUserRoles.get(0).getCaseRole());
        }

        @Test
        @DisplayName("should return empty result for non existing cases")
        void shouldReturnEmptyResultOnNonExistingCases() {
            List<Long> caseReferences = Lists.newArrayList(CASE_NOT_FOUND);
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(caseReferences, Lists.newArrayList());

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

    private void configureCaseRoleRepository() {
        when(caseRoleRepository.getCaseRoles(CASE_TYPE_ID)).thenReturn(Sets.newHashSet(CASE_ROLE,
                                                                                       CASE_ROLE_OTHER,
                                                                                       CASE_ROLE_GRANTED));
    }

    private void configureCaseUserRepository() {
        when(caseUserRepository.findCaseRoles(CASE_ID,
                                              USER_ID)).thenReturn(Collections.singletonList(CASE_ROLE_GRANTED));
        CaseUserEntity caseUserEntity = createCaseUserEntity(CASE_ID, CASE_ROLE, USER_ID);
        when(caseUserRepository.findCaseUserRoles(anyList(), anyList())).thenReturn(Collections.singletonList(caseUserEntity));
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
