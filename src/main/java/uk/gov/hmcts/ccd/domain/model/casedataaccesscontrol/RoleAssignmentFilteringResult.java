package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;

public class RoleAssignmentFilteringResult {

    private final List<Pair<RoleAssignment, RoleMatchingResult>> roleAssignmentRoleMatchingResults;

    public RoleAssignmentFilteringResult(List<Pair<RoleAssignment, RoleMatchingResult>> roleAssignmentMatchPairs) {
        this.roleAssignmentRoleMatchingResults = roleAssignmentMatchPairs;
    }

    public List<Pair<RoleAssignment, RoleMatchingResult>> getRoleAssignmentRoleMatchingResults() {
        return roleAssignmentRoleMatchingResults;
    }

    public List<RoleAssignment> getRoleAssignments() {
        return roleAssignmentRoleMatchingResults.stream()
            .map(Pair::getKey)
            .collect(Collectors.toList());
    }

    public boolean atLeastOneCaseRoleExists() {
        return roleAssignmentRoleMatchingResults.stream()
            .map(Pair::getKey)
            .anyMatch(RoleAssignment::isCaseRoleAssignment);
    }
}
