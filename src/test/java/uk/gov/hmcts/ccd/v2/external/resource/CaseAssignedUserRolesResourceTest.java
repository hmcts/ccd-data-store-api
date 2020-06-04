package uk.gov.hmcts.ccd.v2.external.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CaseAssignedUserRolesResourceTest {

    private final Long caseId1 = 12345678767L;
    private final Long caseId2 = 12345678768L;
    private final String caseIds = caseId1 + "," + caseId2;

    private final String userId1 = "Test-User1";
    private final String userId2 = "Test-User2";
    private final String userIds = userId1 + "," + userId2;

    private final String linkSelfForCaseAssignedCUserRoles = String.format("/case-users?case_ids=%s&user_ids=%s", caseIds, userIds);

    @Test
    @DisplayName("should copy case assigned user role data")
    public void shouldCopyCaseAssignedUserRoleContent() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = createCaseAssignedUserRoles();

        CaseAssignedUserRolesResource resource = new CaseAssignedUserRolesResource(caseIds, userIds, caseAssignedUserRoles);

        assertAll(
            () -> assertThat(resource.getCaseAssignedUserRoles().size(), equalTo(2))
        );
    }

    @Test
    @DisplayName("should link to itself")
    public void shouldLinkToSelf() {
        final CaseAssignedUserRolesResource resource = new CaseAssignedUserRolesResource(caseIds, userIds, createCaseAssignedUserRoles());

        Optional<Link> self = resource.getLink("self");
        MatcherAssert.assertThat(self.get().getHref(), equalTo(linkSelfForCaseAssignedCUserRoles));
    }

    private List<CaseAssignedUserRole> createCaseAssignedUserRoles() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = new ArrayList<>();
        caseAssignedUserRoles.add(new CaseAssignedUserRole(caseId1, userId1, "caseworker-caa"));
        caseAssignedUserRoles.add(new CaseAssignedUserRole(caseId2, userId2, "caseworker-caa"));
        return caseAssignedUserRoles;
    }

}
