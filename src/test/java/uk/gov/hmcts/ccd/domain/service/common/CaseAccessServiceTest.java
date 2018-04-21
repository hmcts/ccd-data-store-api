package uk.gov.hmcts.ccd.domain.service.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

class CaseAccessServiceTest {

    private static final String USER_ID = "69";

    private static final Long CASE_GRANTED_1_ID = 123L;
    private static final Long CASE_GRANTED_2_ID = 456L;
    private static final Long CASE_REVOKED_ID = 789L;

    private static final List<Long> CASES_GRANTED = Arrays.asList(CASE_GRANTED_1_ID, CASE_GRANTED_2_ID);

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
    class whenSolicitor {

        private final String[] ROLES = {
            "somethingThatIsNotASolicitor",
            "somethingThatIsA-solicitor"
        };

        @BeforeEach
        void setUp() {
            withRoles(ROLES);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void userHasSolicitorRoleAndAccessGranted_caseVisible() {
            assertAccessGranted(caseGranted());
        }

        @Test
        @DisplayName("should return false if access was revoked")
        void userHasSolicitorRoleAndAccessRevoked_caseNotVisible() {
            assertAccessRevoked(caseRevoked());
        }

        @Test
        @DisplayName("should give GRANTED access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(serviceAndUserDetails(ROLES));
            assertThat(accessLevel, equalTo(AccessLevel.GRANTED));
        }
    }

    @Nested
    @DisplayName("when user is a citizen")
    class whenCitizen {

        private final String[] ROLES = {
            "citizen",
            "probate-private-beta"
        };

        @BeforeEach
        void setUp() {
            withRoles(ROLES);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void userHasCitizenRoleAndAccessGranted_caseVisible() {
            assertAccessGranted(caseGranted());
        }

        @Test
        @DisplayName("should return false if access was revoked")
        void userHasCitizenRoleAndAccessRevoked_caseNotVisible() {
            assertAccessRevoked(caseRevoked());
        }

        @Test
        @DisplayName("should return false if no access granted")
        void userHasNoAccessGranted_caseNotVisible() {
            noAccessGranted();

            assertAccessRevoked(caseGranted());
        }

        @Test
        @DisplayName("should give GRANTED access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(serviceAndUserDetails(ROLES));
            assertThat(accessLevel, equalTo(AccessLevel.GRANTED));
        }
    }

    @Nested
    @DisplayName("when user is a letter-holder")
    class whenLetterHolder {

        private final String[] ROLES = {
            "letter-holder"
        };

        @BeforeEach
        void setUp() {
            withRoles(ROLES);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void userHasLetterHolderRoleAndAccessGranted_caseVisible() {
            assertAccessGranted(caseGranted());
        }

        @Test
        @DisplayName("should return false if access was revoked")
        void userHasLetterHolderRoleAndAccessRevoked_caseNotVisible() {
            assertAccessRevoked(caseRevoked());
        }

        @Test
        @DisplayName("should give GRANTED access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(serviceAndUserDetails(ROLES));
            assertThat(accessLevel, equalTo(AccessLevel.GRANTED));
        }
    }

    @Nested
    @DisplayName("when user is a citizen-loaX")
    class whenCitizenLoaX {

        private final String[] ROLES = {
            "citizen-loaX"
        };

        @BeforeEach
        void setUp() {
            withRoles(ROLES);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void userHasCitizenRoleAndAccessGranted_caseVisible() {
            assertAccessGranted(caseGranted());
        }

        @Test
        @DisplayName("should return false if access was revoked")
        void userHasCitizenRoleAndAccessRevoked_caseNotVisible() {
            assertAccessRevoked(caseRevoked());
        }

        @Test
        @DisplayName("should give GRANTED access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(serviceAndUserDetails(ROLES));
            assertThat(accessLevel, equalTo(AccessLevel.GRANTED));
        }
    }

    @Nested
    @DisplayName("when user is NOT a citizen or a solicitor")
    class whenOther {

        private final String[] ROLES = {
            "caseworker-divorce",
            "not-citizen",
            "somethingThatIsNotASolicitor",
            "notASolicitorAsCapitalS-Solicitor",
            "notASolicitorAsTrailingSpace-solicitor "
        };

        @BeforeEach
        void setUp() {
            withRoles(ROLES);
        }

        @Test
        @DisplayName("should return true if access was granted")
        void shouldGrantAccessToGrantedCase() {
            assertAccessGrantedWithoutChecks(caseGranted());
        }

        @Test
        @DisplayName("should return true if access was revoked")
        void shouldGrantAccessToRevokedCase() {
            assertAccessGrantedWithoutChecks(caseRevoked());
        }

        @Test
        @DisplayName("should give ALL access level")
        void accessLevel() {
            final AccessLevel accessLevel = caseAccessService.getAccessLevel(serviceAndUserDetails(ROLES));
            assertThat(accessLevel, equalTo(AccessLevel.ALL));
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
            () -> verify(userRepository).getUserDetails(),
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
        doReturn(idamProperties(USER_ID, roles)).when(userRepository)
                                                .getUserDetails();
    }

    private ServiceAndUserDetails serviceAndUserDetails(String[] roles) {
        return new ServiceAndUserDetails("id", "password", Arrays.asList(roles), "service");
    }

    private IDAMProperties idamProperties(String userId, String[] roles) {
        IDAMProperties idamProperties = new IDAMProperties();
        idamProperties.setRoles(roles);
        idamProperties.setId(userId);
        return idamProperties;
    }

}
