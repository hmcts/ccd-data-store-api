package uk.gov.hmcts.ccd.domain.service.cauroles.rolevalidator;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class DefaultCaseAssignedUserRoleValidatorTest {

    private final String roleCaseworkerCaa = "caseworker-caa";
    private final String roleCaseworkerSolicitor = "caseworker-solicitor";

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private ApplicationParams applicationParams;

    private DefaultCaseAssignedUserRoleValidator caseAssignedUserRoleValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseAssignedUserRoleValidator = new DefaultCaseAssignedUserRoleValidator(userRepository, applicationParams);
    }

    @Test
    @DisplayName("Can access case roles when user has caseworker-caa")
    void canAccessUserCaseRolesWhenUserRolesContainsValidAccessRole() {
        when(userRepository.anyRoleEqualsAnyOf(any())).thenReturn(true);
        when(applicationParams.getCcdAccessControlCrossJurisdictionRoles())
                .thenReturn(Arrays.asList(roleCaseworkerCaa));
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(Lists.newArrayList());
        assertTrue(canAccess);
    }

    @Test
    @DisplayName("Can access self case user roles")
    void canAccessSelfUserCaseRolesWhenSelfUserIdPassed() {
        mockAccessProfiles();
        when(userRepository.getUserId()).thenReturn("1234567");
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(Lists.newArrayList("1234567"));
        assertTrue(canAccess);
    }

    private void mockAccessProfiles() {
        when(caseDataAccessControl.generateAccessProfilesByCaseTypeId(anyString()))
            .thenReturn(createAccessProfiles(Collections.singleton(roleCaseworkerSolicitor)));
    }

    private Set<AccessProfile> createAccessProfiles(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> AccessProfile.builder().readOnly(false)
                .accessProfile(userRole)
                .build())
            .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Can access self case user roles even same user id passed more than once")
    void canAccessSelfUserCaseRolesWhenSelfUserIdPassedMoreThanOnce() {
        mockAccessProfiles();
        when(userRepository.getUserId()).thenReturn("1234567");
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(
                Lists.newArrayList("1234567", "1234567"));
        assertTrue(canAccess);
    }

    @Test
    @DisplayName("Can not access other user case roles")
    void canNotAccessOtherUserCaseRolesWhenMoreUserIdsPassedOtherThanSelf() {
        mockAccessProfiles();
        when(userRepository.getUserId()).thenReturn("1234567");
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(
                Lists.newArrayList("1234567", "1234568"));
        assertFalse(canAccess);
    }

    @Test
    @DisplayName("Can not access other user case roles when self id not passed")
    void canNotAccessOtherUserCaseRolesWhenSelfUserIdNotPassed() {
        mockAccessProfiles();
        when(userRepository.getUserId()).thenReturn("1234567");
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(Lists.newArrayList("1234568"));
        assertFalse(canAccess);
    }
}
