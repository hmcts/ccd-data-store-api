package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class RoleAssignmentFilteringResult {

    private final List<Pair<RoleAssignment, RoleMatchingResult>> roleAssignmentRoleMatchingResults;

    public RoleAssignmentFilteringResult(List<Pair<RoleAssignment, RoleMatchingResult>> roleAssignmentMatchPairs) {
        this.roleAssignmentRoleMatchingResults = roleAssignmentMatchPairs;
    }

    public List<Pair<RoleAssignment, RoleMatchingResult>> getRoleAssignmentRoleMatchingResults() {
        return roleAssignmentRoleMatchingResults;
    }
}
