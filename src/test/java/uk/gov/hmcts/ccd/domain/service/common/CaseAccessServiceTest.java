package uk.gov.hmcts.ccd.domain.service.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseAccessServiceTest {

    private static final String USER_ID = "69";

    private static final String CASE_GRANTED_1_ID = "123";
    private static final String CASE_GRANTED_2_ID = "456";
    private static final String CASE_ROLE_CREATOR_ID = "[CREATOR]";
    private static final String CASE_REVOKED_ID = "789";

    private static final List<Long> CASES_GRANTED = asList(Long.valueOf(CASE_GRANTED_1_ID), Long.valueOf(CASE_GRANTED_2_ID));

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseUserRepository caseUserRepository;

    @InjectMocks
    private CaseAccessService caseAccessService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        grantAccess();
    }

    @Nested
    @DisplayName("when user is a solicitor")
    class WhenSolicitor {

        private final String[] roles = {
            "somethingThatIsNotASolicitor",
            "somethingThatIsA-solicitor"
        };

        @BeforeEach
        void setUp() {
            withRoles(roles);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void userHasSolicitorRoleAndAccessGrantedCaseVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            assertAccessGranted(caseGranted());
        }

        @Test
        @DisplayName("should return false if access was revoked")
        void userHasSolicitorRoleAndAccessRevokedCaseNotVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            assertAccessRevoked(caseRevoked());
        }

        @Test
        @DisplayName("should give GRANTED access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(userInfo(roles));
            assertThat(accessLevel, equalTo(AccessLevel.GRANTED));
        }
    }

    @Nested
    @DisplayName("when user is from local authority")
    class WhenLocalAuthority {

        private final String[] roles = {
            "caseworker-superdupajurisdiction-localAuthority"
        };

        @BeforeEach
        void setUp() {
            withRoles(roles);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void userHasSolicitorRoleAndAccessGrantedCaseVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            assertAccessGranted(caseGranted());
        }

        @Test
        @DisplayName("should return false if access was revoked")
        void userHasSolicitorRoleAndAccessRevokedCaseNotVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            assertAccessRevoked(caseRevoked());
        }

        @Test
        @DisplayName("should give GRANTED access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(userInfo(roles));
            assertThat(accessLevel, equalTo(AccessLevel.GRANTED));
        }
    }

    @Nested
    @DisplayName("when user is not from local authority")
    class WhenNotLocalAuthority {

        private final String[] roles = {
            "caseworker-publiclaw-localauthority"
        };

        @BeforeEach
        void setUp() {
            withRoles(roles);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void shouldGrantAccessToGrantedCase() {
            doReturn(false).when(userRepository).anyRoleMatches(any());
            assertAccessGrantedWithoutChecks(caseGranted());
        }

        @Test
        @DisplayName("should return true if access was revoked")
        void shouldGrantAccessToRevokedCase() {
            doReturn(false).when(userRepository).anyRoleMatches(any());
            assertAccessGrantedWithoutChecks(caseRevoked());
        }

        @Test
        @DisplayName("should give ALL access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(userInfo(roles));
            assertThat(accessLevel, equalTo(AccessLevel.ALL));
        }
    }

    @Nested
    @DisplayName("when user is a panel member")
    class WhenPanelMember {

        private final String[] roles = {
            "other",
            "some-panelmember"
        };

        @BeforeEach
        void setUp() {
            withRoles(roles);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void userHasPanelMemberRoleAndAccessGrantedCaseVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            assertAccessGranted(caseGranted());
        }

        @Test
        @DisplayName("should return false if access was revoked")
        void userHasPanelMemberRoleAndAccessRevokedCaseNotVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            assertAccessRevoked(caseRevoked());
        }

        @Test
        @DisplayName("should give GRANTED access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(userInfo(roles));
            assertThat(accessLevel, equalTo(AccessLevel.GRANTED));
        }
    }

    @Nested
    @DisplayName("when user is a citizen")
    class WhenCitizen {

        private final String[] roles = {
            "citizen",
            "probate-private-beta"
        };

        @BeforeEach
        void setUp() {
            withRoles(roles);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void userHasCitizenRoleAndAccessGrantedCaseVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            assertAccessGranted(caseGranted());
        }

        @Test
        @DisplayName("should return false if access was revoked")
        void userHasCitizenRoleAndAccessRevokedCaseNotVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            assertAccessRevoked(caseRevoked());
        }

        @Test
        @DisplayName("should return false if no access granted")
        void userHasNoAccessGranted_caseNotVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            noAccessGranted();

            assertAccessRevoked(caseGranted());
        }

        @Test
        @DisplayName("should give GRANTED access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(userInfo(roles));
            assertThat(accessLevel, equalTo(AccessLevel.GRANTED));
        }
    }

    @Nested
    @DisplayName("when user is a letter-holder")
    class WhenLetterHolder {

        private final String[] roles = {
            "letter-holder"
        };

        @BeforeEach
        void setUp() {
            withRoles(roles);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void userHasLetterHolderRoleAndAccessGrantedCaseVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            assertAccessGranted(caseGranted());
        }

        @Test
        @DisplayName("should return false if access was revoked")
        void userHasLetterHolderRoleAndAccessRevokedCaseNotVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            assertAccessRevoked(caseRevoked());
        }

        @Test
        @DisplayName("should give GRANTED access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(userInfo(roles));
            assertThat(accessLevel, equalTo(AccessLevel.GRANTED));
        }
    }

    @Nested
    @DisplayName("when user is a citizen-loaX")
    class WhenCitizenLoaX {

        private final String[] roles = {
            "citizen-loaX"
        };

        @BeforeEach
        void setUp() {
            withRoles(roles);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void userHasCitizenRoleAndAccessGrantedCaseVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            assertAccessGranted(caseGranted());
        }

        @Test
        @DisplayName("should return false if access was revoked")
        void userHasCitizenRoleAndAccessRevokedCaseNotVisible() {
            doReturn(true).when(userRepository).anyRoleMatches(any());
            assertAccessRevoked(caseRevoked());
        }

        @Test
        @DisplayName("should give GRANTED access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(userInfo(roles));
            assertThat(accessLevel, equalTo(AccessLevel.GRANTED));
        }
    }

    @Nested
    @DisplayName("when user is NOT a citizen or a solicitor")
    class WhenOther {

        private final String[] roles = {
            "caseworker-divorce",
            "not-citizen",
            "somethingThatIsNotASolicitor",
            "notASolicitorAsCapitalS-Solicitor",
            "notASolicitorAsTrailingSpace-solicitor "
        };

        @BeforeEach
        void setUp() {
            withRoles(roles);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void shouldGrantAccessToGrantedCase() {
            doReturn(false).when(userRepository).anyRoleMatches(any());
            assertAccessGrantedWithoutChecks(caseGranted());
        }

        @Test
        @DisplayName("should return true if access was revoked")
        void shouldGrantAccessToRevokedCase() {
            doReturn(false).when(userRepository).anyRoleMatches(any());
            assertAccessGrantedWithoutChecks(caseRevoked());
        }

        @Test
        @DisplayName("should give ALL access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(userInfo(roles));
            assertThat(accessLevel, equalTo(AccessLevel.ALL));
        }
    }

    @Nested
    @DisplayName("getGrantedCaseIdsForRestrictedRoles()")
    class GetGrantedCaseIdsForRestrictedRoles {

        @Nested
        @DisplayName("when a solicitor")
        class WhenSolicitor {
            private final String[] roles = {
                "judiciary-solicitor"
            };

            @BeforeEach
            void setUp() {
                withRoles(roles);
            }

            @Test
            @DisplayName("should return granted case ids for user with solicitor role")
            void shouldReturnCaseIds() {
                doReturn(true).when(userRepository).anyRoleMatches(any());
                Optional<List<Long>> result = caseAccessService.getGrantedCaseIdsForRestrictedRoles();

                assertThat(result.isPresent(), is(true));
                assertGrantedCaseIds(result.get());
            }
        }

        @Nested
        @DisplayName("when a citizen")
        class WhenCitizen {
            private final String[] roles = {
                "citizen"
            };

            @BeforeEach
            void setUp() {
                withRoles(roles);
            }

            @Test
            @DisplayName("should return granted case ids for user with citizen role")
            void shouldReturnCaseIds() {
                doReturn(true).when(userRepository).anyRoleMatches(any());
                Optional<List<Long>> result = caseAccessService.getGrantedCaseIdsForRestrictedRoles();

                assertThat(result.isPresent(), is(true));
                assertGrantedCaseIds(result.get());
            }
        }

        @Nested
        @DisplayName("when a letter holder")
        class WhenLetterHolder {
            private final String[] roles = {
                "letter-holder"
            };

            @BeforeEach
            void setUp() {
                withRoles(roles);
            }

            @Test
            @DisplayName("should return granted case ids for user with letter-holder role")
            void shouldReturnCaseIds() {
                doReturn(true).when(userRepository).anyRoleMatches(any());
                Optional<List<Long>> result = caseAccessService.getGrantedCaseIdsForRestrictedRoles();
                assertThat(result.isPresent(), is(true));
                assertGrantedCaseIds(result.get());
            }
        }

        @Nested
        @DisplayName("when a case worker")
        class WhenCaseWorker {
            private final String[] roles = {
                "caseworker-divorce"
            };

            @BeforeEach
            void setUp() {
                withRoles(roles);
            }

            @Test
            @DisplayName("should return no case ids for user with case worker role")
            void shouldReturnCaseIds() {
                Optional<List<Long>> result = caseAccessService.getGrantedCaseIdsForRestrictedRoles();

                assertThat(result.isPresent(), is(false));
            }
        }

        private void assertGrantedCaseIds(List<Long> result) {
            assertAll(
                () -> assertThat(result, hasItems(Long.valueOf(CASE_GRANTED_1_ID), Long.valueOf(CASE_GRANTED_2_ID))),
                () -> verify(userRepository).getUserId(),
                () -> verify(userRepository).anyRoleMatches(any()),
                () -> verify(caseUserRepository).findCasesUserIdHasAccessTo(USER_ID)
            );
        }
    }


    @Nested
    @DisplayName("case roles")
    class CaseRoleTest {
        private final List<String> caseRoles = asList("[CASE_ROLE_1]", "[CASE_ROLE_2]");

        @BeforeEach
        void setUp() {
            doReturn(USER_ID).when(userRepository).getUserId();
            doReturn(caseRoles).when(caseUserRepository).findCaseRoles(Long.valueOf(CASE_GRANTED_1_ID), USER_ID);
        }

        @Test
        @DisplayName("get case roles for a case Id")
        void getCaseRoles() {
            Set<String> caseRoles = caseAccessService.getCaseRoles(CASE_GRANTED_1_ID);

            assertAll(
                () -> assertThat(caseRoles.size(), Is.is(2)),
                () -> assertThat(caseRoles, hasItems("[CASE_ROLE_1]", "[CASE_ROLE_2]"))
            );
        }
    }

    @Nested
    @DisplayName("user roles")
    class UserRoleTest {
        @BeforeEach
        void setUp() {
            doReturn(Sets.newHashSet(CASE_GRANTED_1_ID, CASE_GRANTED_2_ID)).when(userRepository).getUserRoles();
        }

        @Test
        @DisplayName("should return user roles")
        void getCaseRoles() {
            Set<String> caseRoles = caseAccessService.getUserRoles();

            assertAll(
                () -> assertThat(caseRoles.size(), Is.is(2)),
                () -> assertThat(caseRoles, hasItems(CASE_GRANTED_1_ID, CASE_GRANTED_2_ID))
            );
        }

        @Test
        @DisplayName("should return case creation user roles")
        void getCaseCreationCaseRoles() {
            Set<String> caseRoles = caseAccessService.getCaseCreationRoles();

            assertAll(
                () -> assertThat(caseRoles.size(), Is.is(3)),
                () -> assertThat(caseRoles, hasItems(CASE_GRANTED_1_ID, CASE_GRANTED_2_ID, CASE_ROLE_CREATOR_ID)));
        }

        @Test
        @DisplayName("should throw exception when no user role found")
        void getCaseRolesThrows() {
            doReturn(null).when(userRepository).getUserRoles();

            assertThrows(ValidationException.class, () -> caseAccessService.getUserRoles());
        }
    }

    @Nested
    @DisplayName("create case roles")
    class CreateCaseRolesTest {
        @BeforeEach
        void setUp() {
            doReturn(Sets.newHashSet(CASE_GRANTED_1_ID, CASE_GRANTED_2_ID)).when(userRepository).getUserRoles();
        }

        @Test
        @DisplayName("should return create user roles")
        void getCreateCaseRoles() {
            Set<String> caseRoles = caseAccessService.getCaseCreationRoles();

            assertAll(
                () -> assertThat(caseRoles.size(), Is.is(3)),
                () -> assertThat(caseRoles, hasItems(CASE_GRANTED_1_ID, CASE_GRANTED_2_ID, GlobalCaseRole.CREATOR.getRole()))
            );
        }

        @Test
        @DisplayName("should throw exception when no user role found")
        void getCreateCaseRolesThrows() {
            doReturn(null).when(userRepository).getUserRoles();

            assertThrows(ValidationException.class, () -> caseAccessService.getCaseCreationRoles());
        }
    }

    @Nested
    @DisplayName("jurisdiction access validation")
    class JurisdictionAccessTest {

        @BeforeEach
        void setUp() {
            doReturn(Lists.newArrayList("PROBATE", "DIVORCE")).when(userRepository).getUserRolesJurisdictions();
        }

        @Test
        @DisplayName("should return true when user has access to jurisdiction")
        void shouldReturnTrueWhenUserHasAccess() {
            boolean canAccess = caseAccessService.isJurisdictionAccessAllowed("probate");
            assertTrue(canAccess);
        }

        @Test
        @DisplayName("should return false when user has no access to jurisdiction")
        void shouldReturnFalseWhenUserHasAccess() {
            boolean canAccess = caseAccessService.isJurisdictionAccessAllowed("autotest1");
            assertFalse(canAccess);
        }
    }

    private void assertAccessGranted(CaseDetails caseDetails) {
        assertAccess(caseDetails, true, true);
    }

    private void assertAccessGrantedWithoutChecks(CaseDetails caseDetails) {
        assertAccess(caseDetails, true, false);
    }

    private void assertAccessRevoked(CaseDetails caseDetails) {
        assertAccess(caseDetails, false, true);
    }

    private void assertAccess(CaseDetails caseDetails, Boolean granted, Boolean withChecks) {
        assertAll(
            () -> assertThat(caseAccessService.canUserAccess(caseDetails), is(granted)),
            () -> verify(userRepository).anyRoleMatches(any()),
            () -> verify(caseUserRepository, withChecks ? atLeastOnce() : never()).findCasesUserIdHasAccessTo(USER_ID)
        );
    }

    private void grantAccess() {
        doReturn(CASES_GRANTED)
            .when(caseUserRepository)
            .findCasesUserIdHasAccessTo(USER_ID);
    }

    private void noAccessGranted() {
        doReturn(null)
            .when(caseUserRepository)
            .findCasesUserIdHasAccessTo(USER_ID);
    }

    private CaseDetails caseGranted() {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(CASE_GRANTED_1_ID);
        return caseDetails;
    }

    private CaseDetails caseRevoked() {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(CASE_REVOKED_ID);
        return caseDetails;
    }

    private void withRoles(String... roles) {
        when(userRepository.getUserId()).thenReturn(USER_ID);
        when(userRepository.getUserRoles()).thenReturn(new HashSet<>(asList(roles)));
    }

    private UserInfo userInfo(String[] roles) {
        return UserInfo.builder()
            .roles(Arrays.asList(roles))
            .build();
    }

}
