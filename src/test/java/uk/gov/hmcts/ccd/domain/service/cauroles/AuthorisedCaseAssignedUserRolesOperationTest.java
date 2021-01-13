package uk.gov.hmcts.ccd.domain.service.cauroles;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.ccd.domain.service.cauroles.rolevalidator.CaseAssignedUserRoleValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseRoleAccessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorisedCaseAssignedUserRolesOperationTest {

    private AuthorisedCaseAssignedUserRolesOperation authorisedCaseAssignedUserRolesOperation;

    @Mock
    private DefaultCaseAssignedUserRolesOperation defaultCaseAssignedUserRolesOperation;

    @Mock
    private CaseAssignedUserRoleValidator caseAssignedUserRoleValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        authorisedCaseAssignedUserRolesOperation = new AuthorisedCaseAssignedUserRolesOperation(
            defaultCaseAssignedUserRolesOperation,
            caseAssignedUserRoleValidator);
        when(defaultCaseAssignedUserRolesOperation.findCaseUserRoles(anyList(), anyList()))
                .thenReturn(createCaseAssignedUserRoles());
    }

    @Test
    void shouldCallDefaultAddCaseUserRoles() {
        // ARRANGE
        List<CaseAssignedUserRoleWithOrganisation> caseUserRolesRequests = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(),
            new CaseAssignedUserRoleWithOrganisation()
        );

        // ACT
        authorisedCaseAssignedUserRolesOperation.addCaseUserRoles(caseUserRolesRequests);

        // ASSERT
        verify(defaultCaseAssignedUserRolesOperation).addCaseUserRoles(caseUserRolesRequests);
    }

    @Test
    void shouldReturnCaseAssignedUserRoles() {
        // ARRANGE
        List<Long> caseIds = Lists.newArrayList(123456L);
        List<String> userIds = Lists.newArrayList("234567");
        when(caseAssignedUserRoleValidator.canAccessUserCaseRoles(anyList())).thenReturn(true);

        // ACT
        List<CaseAssignedUserRole> caseAssignedUserRoles = authorisedCaseAssignedUserRolesOperation
            .findCaseUserRoles(caseIds, userIds);

        // ASSERT
        assertEquals(2, caseAssignedUserRoles.size());
    }

    @Test
    void shouldThrowExceptionWhenUserHasNotPermissions() {
        // ARRANGE
        List<Long> caseIds = Lists.newArrayList(123456L);
        List<String> userIds = Lists.newArrayList("234567");
        when(caseAssignedUserRoleValidator.canAccessUserCaseRoles(anyList())).thenReturn(false);

        // ACT / ASSERT
        assertThrows(
            CaseRoleAccessException.class, () -> authorisedCaseAssignedUserRolesOperation
                .findCaseUserRoles(caseIds, userIds)
        );
    }

    @Test
    void shouldCallDefaultremoveCaseUserRoles() {
        List<CaseAssignedUserRoleWithOrganisation> caseUserRolesRequests = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(),
                new CaseAssignedUserRoleWithOrganisation()
        );

        authorisedCaseAssignedUserRolesOperation.removeCaseUserRoles(caseUserRolesRequests);

        verify(defaultCaseAssignedUserRolesOperation).removeCaseUserRoles(caseUserRolesRequests);
    }

    private List<CaseAssignedUserRole> createCaseAssignedUserRoles() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = Lists.newArrayList();

        caseAssignedUserRoles.add(new CaseAssignedUserRole());
        caseAssignedUserRoles.add(new CaseAssignedUserRole());
        return caseAssignedUserRoles;
    }
}
