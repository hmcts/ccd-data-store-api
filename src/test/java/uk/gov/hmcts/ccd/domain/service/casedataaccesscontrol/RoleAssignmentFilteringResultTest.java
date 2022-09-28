package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.LocationMatcher;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.RegionMatcher;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class RoleAssignmentFilteringResultTest {

    private static final RoleAssignment roleAssignment = RoleAssignment.builder().build();

    @Test
    void testHasPassedFilteringReturnsTrue() {

        Map<String, Boolean> filteringResults = new HashMap<>();

        filteringResults.put("roleMatcher1", Boolean.TRUE);
        filteringResults.put("roleMatcher2", Boolean.TRUE);
        filteringResults.put("roleMatcher3", Boolean.TRUE);
        filteringResults.put("roleMatcher4", Boolean.TRUE);
        filteringResults.put("roleMatcher5", Boolean.TRUE);

        RoleAssignmentFilteringResult roleAssignmentFilteringResult =
            new RoleAssignmentFilteringResult(roleAssignment, filteringResults);

        assertTrue(roleAssignmentFilteringResult.hasPassedFiltering());
    }

    @Test
    void testHasPassedFilteringReturnsFalse() {
        Map<String, Boolean> filteringResults = new HashMap<>();

        filteringResults.put("roleMatcher1", Boolean.TRUE);
        filteringResults.put("roleMatcher2", Boolean.FALSE);
        filteringResults.put("roleMatcher3", Boolean.TRUE);
        filteringResults.put("roleMatcher4", Boolean.TRUE);
        filteringResults.put("roleMatcher5", Boolean.TRUE);

        RoleAssignmentFilteringResult roleAssignmentFilteringResult =
            new RoleAssignmentFilteringResult(roleAssignment, filteringResults);

        assertFalse(roleAssignmentFilteringResult.hasPassedFiltering());
    }

    @Test
    void testHasPassedFilteringReturnsFalseWhenFilteringResultsEmpty() {
        RoleAssignmentFilteringResult roleAssignmentFilteringResult =
            new RoleAssignmentFilteringResult(roleAssignment, new HashMap<>());

        assertFalse(roleAssignmentFilteringResult.hasPassedFiltering());
    }

    @Test
    void hasFailedFilteringOnRegionAndOrBaseLocation() {
        Map<String, Boolean> filteringResults = new HashMap<>();

        filteringResults.put("roleMatcher1", Boolean.TRUE);
        filteringResults.put("RegionMatcher", Boolean.FALSE);
        filteringResults.put("roleMatcher3", Boolean.TRUE);
        filteringResults.put("roleMatcher4", Boolean.TRUE);
        filteringResults.put("LocationMatcher", Boolean.FALSE);

        RoleAssignmentFilteringResult roleAssignmentFilteringResult =
            new RoleAssignmentFilteringResult(roleAssignment, filteringResults);

        assertTrue(roleAssignmentFilteringResult.hasFailedFilteringOnRegionAndBaseLocation());
    }

    @Test
    void hasFailedFilteringOnRegion() {
        Map<String, Boolean> filteringResults = new HashMap<>();

        filteringResults.put("roleMatcher1", Boolean.TRUE);
        filteringResults.put("RegionMatcher", Boolean.FALSE);
        filteringResults.put("roleMatcher3", Boolean.TRUE);
        filteringResults.put("roleMatcher4", Boolean.TRUE);
        filteringResults.put("LocationMatcher", Boolean.TRUE);

        RoleAssignmentFilteringResult roleAssignmentFilteringResult =
            new RoleAssignmentFilteringResult(roleAssignment, filteringResults);

        assertTrue(roleAssignmentFilteringResult.hasFailedFilteringOnRegionAndBaseLocation());
    }

    @Test
    void hasFailedFilteringOnLocation() {
        Map<String, Boolean> filteringResults = new HashMap<>();

        filteringResults.put("roleMatcher1", Boolean.TRUE);
        filteringResults.put("RegionMatcher", Boolean.TRUE);
        filteringResults.put("roleMatcher3", Boolean.TRUE);
        filteringResults.put("roleMatcher4", Boolean.TRUE);
        filteringResults.put("LocationMatcher", Boolean.FALSE);

        RoleAssignmentFilteringResult roleAssignmentFilteringResult =
            new RoleAssignmentFilteringResult(roleAssignment, filteringResults);

        assertTrue(roleAssignmentFilteringResult.hasFailedFilteringOnRegionAndBaseLocation());
    }

    @Test
    void hasFailedFilteringBasedOnRegionAndLocationAndOtherMatchers() {
        Map<String, Boolean> filteringResults = new HashMap<>();

        filteringResults.put("roleMatcher1", Boolean.FALSE);
        filteringResults.put("RegionMatcher", Boolean.FALSE);
        filteringResults.put("roleMatcher3", Boolean.TRUE);
        filteringResults.put("roleMatcher4", Boolean.TRUE);
        filteringResults.put("LocationMatcher", Boolean.FALSE);

        RoleAssignmentFilteringResult roleAssignmentFilteringResult =
            new RoleAssignmentFilteringResult(roleAssignment, filteringResults);

        assertFalse(roleAssignmentFilteringResult.hasFailedFilteringOnRegionAndBaseLocation());
    }

    @Test
    void hasFailedFilteringBasedOnRegionAndLocationReturnsFalseWhenNoFilteringResults() {
        Map<String, Boolean> filteringResults = new HashMap<>();

        RoleAssignmentFilteringResult roleAssignmentFilteringResult =
            new RoleAssignmentFilteringResult(roleAssignment, filteringResults);

        assertFalse(roleAssignmentFilteringResult.hasFailedFilteringOnRegionAndBaseLocation());
    }

    @Test
    void hasFailedFilteringBasedOnRegionAndLocationReturnsTrueWhenUsingOtherNames() {
        Map<String, Boolean> filteringResults = new HashMap<>();

        filteringResults.put("roleMatcher1", Boolean.TRUE);
        filteringResults.put(RegionMatcher.class.getSimpleName(), Boolean.FALSE);
        filteringResults.put(RegionMatcher.class.getName(), Boolean.FALSE);
        filteringResults.put("roleMatcher3", Boolean.TRUE);
        filteringResults.put("roleMatcher4", Boolean.TRUE);
        filteringResults.put(LocationMatcher.class.getSimpleName(), Boolean.FALSE);
        filteringResults.put(LocationMatcher.class.getName(), Boolean.FALSE);

        RoleAssignmentFilteringResult roleAssignmentFilteringResult =
            new RoleAssignmentFilteringResult(roleAssignment, filteringResults);

        assertTrue(roleAssignmentFilteringResult.hasFailedFilteringOnRegionAndBaseLocation());
    }
}
