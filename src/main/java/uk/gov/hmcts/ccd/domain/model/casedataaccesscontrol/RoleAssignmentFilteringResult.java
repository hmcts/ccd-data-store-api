package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public class RoleAssignmentFilteringResult {

    private final List<Pair<RoleAssignment, RoleMatchingResult>> roleAssignmentMatchingResults;

    public RoleAssignmentFilteringResult(List<Pair<RoleAssignment, RoleMatchingResult>> roleAssignmentMatchPairs) {
        this.roleAssignmentMatchingResults = roleAssignmentMatchPairs;
    }

    public List<Pair<RoleAssignment, RoleMatchingResult>> getRoleAssignmentMatchingResults() {
        return roleAssignmentMatchingResults;
    }

    public List<RoleAssignment> getRoleAssignments() {
        return roleAssignmentMatchingResults.stream()
            .map(Pair::getKey)
            .collect(Collectors.toList());
    }

    public boolean atLeastOneCaseRoleExists() {
        return getRoleAssignments()
            .stream()
            .anyMatch(RoleAssignment::isCaseRoleAssignment);
    }


    public boolean hasGrantTypeExcludedRole() {
        return getRoleAssignments().stream()
            .anyMatch(roleAssignment -> roleAssignment.getGrantType().equals(GrantType.EXCLUDED.name()));
    }

    public RoleAssignmentFilteringResult retainBasicAndSpecificGrantTypeRolesOnly() {
        return new RoleAssignmentFilteringResult(roleAssignmentMatchingResults
            .stream()
            .filter(pair ->
                pair.getKey().getGrantType().equals(GrantType.BASIC.name())
            || pair.getKey().getGrantType().equals(GrantType.SPECIFIC.name()))
            .collect(Collectors.toList()));
    }
}
