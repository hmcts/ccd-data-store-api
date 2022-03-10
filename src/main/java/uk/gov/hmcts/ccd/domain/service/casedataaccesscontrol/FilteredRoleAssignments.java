package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FilteredRoleAssignments {
    List<RoleAssignmentFilteringResult> roleAssignmentFilteringResults = new ArrayList<>();

    public void addFilterMatchingResult(RoleAssignmentFilteringResult roleAssignmentFilteringResult) {
        roleAssignmentFilteringResults.add(roleAssignmentFilteringResult);
    }

    public List<RoleAssignment> getFilteredMatchingRoleAssignments() {
        return roleAssignmentFilteringResults.stream()
            .filter(RoleAssignmentFilteringResult::hasPassedFiltering)
            .map(RoleAssignmentFilteringResult::getRoleAssignment)
            .collect(Collectors.toList());
    }

    public List<RoleAssignment> getFilteredRoleAssignmentsFailedOnRegionOrBaseLocationMatcher() {
        return roleAssignmentFilteringResults.stream()
            .filter(roleAssignmentFilteringResult ->
                GrantType.STANDARD.name()
                    .equals(roleAssignmentFilteringResult.getRoleAssignment().getGrantType()))
            .filter(RoleAssignmentFilteringResult::hasFailedFilteringOnRegionAndBaseLocation)
            .map(RoleAssignmentFilteringResult::getRoleAssignment)
            .distinct()
            .collect(Collectors.toList());
    }
}
