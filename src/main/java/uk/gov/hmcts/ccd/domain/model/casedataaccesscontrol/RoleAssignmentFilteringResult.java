package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public class RoleAssignmentFilteringResult {

    private final List<Pair<RoleAssignment, RoleMatchingResult>> roleMatchingResults;

    public RoleAssignmentFilteringResult(List<Pair<RoleAssignment, RoleMatchingResult>> roleAssignmentMatchPairs) {
        this.roleMatchingResults = roleAssignmentMatchPairs;
    }

    public List<Pair<RoleAssignment, RoleMatchingResult>> getRoleMatchingResults() {
        return roleMatchingResults;
    }

    public List<RoleAssignment> getRoleAssignments() {
        return roleMatchingResults.stream()
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

    public RoleAssignmentFilteringResult retainBasicAndStandardGrantTypeRolesOnly() {
        return new RoleAssignmentFilteringResult(roleMatchingResults
            .stream()
            .filter(pair ->
                pair.getKey().getGrantType().equals(GrantType.BASIC.name())
            || pair.getKey().getGrantType().equals(GrantType.STANDARD.name()))
            .collect(Collectors.toList()));
    }
}
