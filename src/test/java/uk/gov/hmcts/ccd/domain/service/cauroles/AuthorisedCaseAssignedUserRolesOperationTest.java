package uk.gov.hmcts.ccd.domain.service.cauroles;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
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
        when(defaultCaseAssignedUserRolesOperation.findCaseUserRoles(anyList(), anyList())).thenReturn(createCaseAssignedUserRoles());
    }

    @Test
    void shouldCallDefaultAddCaseUserRoles() {
        // ARRANGE
        List<CaseAssignedUserRole> caseAssignedUserRoles = createCaseAssignedUserRoles();

        // ACT
        authorisedCaseAssignedUserRolesOperation.addCaseUserRoles(caseAssignedUserRoles);

        // ASSERT
        verify(defaultCaseAssignedUserRolesOperation).addCaseUserRoles(caseAssignedUserRoles);
    }

    @Test
    void shouldReturnCaseAssignedUserRoles() {
        when(caseAssignedUserRoleValidator.canAccessUserCaseRoles(anyList())).thenReturn(true);
        List<CaseAssignedUserRole> caseAssignedUserRoles = authorisedCaseAssignedUserRolesOperation
            .findCaseUserRoles(Lists.newArrayList(123456L), Lists.newArrayList("234567"));
        assertEquals(2, caseAssignedUserRoles.size());
    }

    @Test
    void shouldThrowExceptionWhenUserHasNotPermissions() {
        when(caseAssignedUserRoleValidator.canAccessUserCaseRoles(anyList())).thenReturn(false);
        assertThrows(
            CaseRoleAccessException.class, () -> authorisedCaseAssignedUserRolesOperation
                .findCaseUserRoles(Lists.newArrayList(123456L), Lists.newArrayList("234567"))
        );
    }

    private List<CaseAssignedUserRole> createCaseAssignedUserRoles() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = Lists.newArrayList();

        caseAssignedUserRoles.add(new CaseAssignedUserRole());
        caseAssignedUserRoles.add(new CaseAssignedUserRole());
        return caseAssignedUserRoles;
    }
}
