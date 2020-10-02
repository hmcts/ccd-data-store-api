package uk.gov.hmcts.ccd.domain.service.cauroles;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.ccd.domain.service.caseaccess.CaseAccessOperation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultCaseAssignedUserRolesOperationTest {

    private DefaultCaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    @Mock
    private CaseAccessOperation caseAccessOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseAssignedUserRolesOperation = new DefaultCaseAssignedUserRolesOperation(caseAccessOperation);
    }

    @Test
    void addCaseUserRoles() {
        // ARRANGE
        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(),
            new CaseAssignedUserRoleWithOrganisation()
        );

        // ACT
        caseAssignedUserRolesOperation.addCaseUserRoles(caseUserRoles);

        // ASSERT
        verify(caseAccessOperation).addCaseUserRoles(caseUserRoles);
    }

    @Test
    void findCaseUserRoles() {
        when(caseAssignedUserRolesOperation.findCaseUserRoles(anyList(), anyList())).thenReturn(
                createCaseAssignedUserRoles());

        List<CaseAssignedUserRole> caseAssignedUserRoles = caseAssignedUserRolesOperation
            .findCaseUserRoles(Lists.newArrayList(), Lists.newArrayList());

        assertEquals(2, caseAssignedUserRoles.size());
    }

    @Test
    void removeCaseUserRoles() {
        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(),
                new CaseAssignedUserRoleWithOrganisation()
        );

        caseAssignedUserRolesOperation.removeCaseUserRoles(caseUserRoles);

        verify(caseAccessOperation).removeCaseUserRoles(caseUserRoles);
    }

    private List<CaseAssignedUserRole> createCaseAssignedUserRoles() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = Lists.newArrayList();

        caseAssignedUserRoles.add(new CaseAssignedUserRole());
        caseAssignedUserRoles.add(new CaseAssignedUserRole());
        return caseAssignedUserRoles;
    }
}
