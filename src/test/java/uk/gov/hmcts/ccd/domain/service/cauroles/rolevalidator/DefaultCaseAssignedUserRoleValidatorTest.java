package uk.gov.hmcts.ccd.domain.service.cauroles.rolevalidator;

import com.google.common.collect.Lists;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.user.UserRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DefaultCaseAssignedUserRoleValidatorTest {

    private final String roleCaseworkerCaa = "caseworker-caa";
    private final String roleCaseworkerSolicitor = "caseworker-solicitor";

    @Mock
    private UserRepository userRepository;

    private DefaultCaseAssignedUserRoleValidator caseAssignedUserRoleValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseAssignedUserRoleValidator = new DefaultCaseAssignedUserRoleValidator(userRepository);
    }

    @Test
    @DisplayName("Can access case roles when user has caseworker-caa")
    void canAccessUserCaseRolesWhenUserRolesContainsValidAccessRole() {
        when(userRepository.getUserRoles()).thenReturn(Collections.singleton(roleCaseworkerCaa));
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(Lists.newArrayList());
        assertTrue(canAccess);
    }

    @Test
    @DisplayName("Can access self case user roles")
    void canAccessSelfUserCaseRolesWhenSelfUserIdPassed() {
        when(userRepository.getUserRoles()).thenReturn(Collections.singleton(roleCaseworkerSolicitor));
        when(userRepository.getUserId()).thenReturn("1234567");
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(Lists.newArrayList("1234567"));
        assertTrue(canAccess);
    }

    @Test
    @DisplayName("Can access self case user roles even same user id passed more than once")
    void canAccessSelfUserCaseRolesWhenSelfUserIdPassedMoreThanOnce() {
        when(userRepository.getUserRoles()).thenReturn(Collections.singleton(roleCaseworkerSolicitor));
        when(userRepository.getUserId()).thenReturn("1234567");
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(Lists.newArrayList("1234567", "1234567"));
        assertTrue(canAccess);
    }

    @Test
    @DisplayName("Can not access other user case roles")
    void canNotAccessOtherUserCaseRolesWhenMoreUserIdsPassedOtherThanSelf() {
        when(userRepository.getUserRoles()).thenReturn(Collections.singleton(roleCaseworkerSolicitor));
        when(userRepository.getUserId()).thenReturn("1234567");
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(Lists.newArrayList("1234567", "1234568"));
        assertFalse(canAccess);
    }

    @Test
    @DisplayName("Can not access other user case roles when self id not passed")
    void canNotAccessOtherUserCaseRolesWhenSelfUserIdNotPassed() {
        when(userRepository.getUserRoles()).thenReturn(Collections.singleton(roleCaseworkerSolicitor));
        when(userRepository.getUserId()).thenReturn("1234567");
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(Lists.newArrayList("1234568"));
        assertFalse(canAccess);
    }
}
