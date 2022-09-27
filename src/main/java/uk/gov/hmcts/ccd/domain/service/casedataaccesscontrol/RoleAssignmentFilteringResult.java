package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.LocationMatcher;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.RegionMatcher;

import java.util.Map;

@Data
@AllArgsConstructor
public class RoleAssignmentFilteringResult {
    private RoleAssignment roleAssignment;
    private Map<String, Boolean> filterResults;

    private static boolean isRegionOrLocationFilteringResult(Map.Entry<String, Boolean> mapEntry) {
        return mapEntry.getKey().equals(RegionMatcher.class.getSimpleName())
            || mapEntry.getKey().equals(RegionMatcher.class.getName())
            || mapEntry.getKey().equals(LocationMatcher.class.getSimpleName())
            || mapEntry.getKey().equals(LocationMatcher.class.getName());
    }

    public boolean hasPassedFiltering() {
        if (filterResults.values().isEmpty()) {
            return false;
        }
        return filterResults.values().stream().allMatch(filterResult -> filterResult.equals(Boolean.TRUE));
    }

    public boolean hasFailedFilteringOnRegionAndBaseLocation() {
        if (filterResults.values().isEmpty()) {
            return false;
        }

        return filterResults.entrySet().stream()
            .filter(entrySet -> entrySet.getValue().equals(Boolean.FALSE))
            .allMatch(RoleAssignmentFilteringResult::isRegionOrLocationFilteringResult);
    }
}

