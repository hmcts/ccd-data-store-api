package uk.gov.hmcts.ccd.v2.external.resource;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CaseAssignedUserRolesResourceTest {

    private final String caseId1 = "12345678767";
    private final String caseId2 = "12345678768";

    private final String userId1 = "Test-User1";
    private final String userId2 = "Test-User2";

    @Test
    @DisplayName("should copy case assigned user role data")
    void shouldCopyCaseAssignedUserRoleContent() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = createCaseAssignedUserRoles();

        CaseAssignedUserRolesResource resource = new CaseAssignedUserRolesResource(caseAssignedUserRoles);

        assertAll(
            () -> assertThat(resource.getCaseAssignedUserRoles(), hasSize(2))
        );
    }

    @Test
    @DisplayName("should have no links")
    void shouldHaveNoLinks() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = createCaseAssignedUserRoles();

        CaseAssignedUserRolesResource resource = new CaseAssignedUserRolesResource(caseAssignedUserRoles);

        assertAll(
            () -> assertThat(resource.getLinks().toList(), hasSize(0))
        );
    }

    private List<CaseAssignedUserRole> createCaseAssignedUserRoles() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = new ArrayList<>();
        caseAssignedUserRoles.add(new CaseAssignedUserRole(caseId1, userId1, "caseworker-caa"));
        caseAssignedUserRoles.add(new CaseAssignedUserRole(caseId2, userId2, "caseworker-caa"));
        return caseAssignedUserRoles;
    }

}
