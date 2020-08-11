package uk.gov.hmcts.ccd.domain.service.cauroles;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
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
        List<CaseAssignedUserRole> caseAssignedUserRoles = createCaseAssignedUserRoles();

        // ACT
        caseAssignedUserRolesOperation.addCaseUserRoles(caseAssignedUserRoles);

        // ASSERT
        verify(caseAccessOperation).addCaseUserRoles(caseAssignedUserRoles);
    }

    @Test
    void findCaseUserRoles() {
        when(caseAssignedUserRolesOperation.findCaseUserRoles(anyList(), anyList())).thenReturn(createCaseAssignedUserRoles());

        List<CaseAssignedUserRole> caseAssignedUserRoles = caseAssignedUserRolesOperation
            .findCaseUserRoles(Lists.newArrayList(), Lists.newArrayList());

        assertEquals(2, caseAssignedUserRoles.size());
    }

    private List<CaseAssignedUserRole> createCaseAssignedUserRoles() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = Lists.newArrayList();

        caseAssignedUserRoles.add(new CaseAssignedUserRole());
        caseAssignedUserRoles.add(new CaseAssignedUserRole());
        return caseAssignedUserRoles;
    }
}
