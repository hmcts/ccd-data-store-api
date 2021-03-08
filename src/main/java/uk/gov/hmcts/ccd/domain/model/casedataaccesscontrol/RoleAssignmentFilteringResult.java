package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public class RoleAssignmentFilteringResult implements Iterator {

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


    public boolean hasGrantTypeExcluded() {
        return getRoleAssignments().stream()
            .anyMatch(roleAssignment -> roleAssignment.getGrantType().equals(GrantType.EXCLUDED.name()));
    }

    public RoleAssignmentFilteringResult getBasicAndStandardGrantTypeRoles() {
        return new RoleAssignmentFilteringResult(roleAssignmentRoleMatchingResults
            .stream()
            .filter(pair ->
                pair.getKey().getGrantType().equals(GrantType.BASIC.name())
            || pair.getKey().getGrantType().equals(GrantType.STANDARD.name()))
            .collect(Collectors.toList()));
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public Object next() {
        return null;
    }
}
