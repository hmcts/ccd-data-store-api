package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilteredRoleAssignmentsTest {

    @DisplayName("Check RoleAssignments that have passed filtering are returned as expected")
    @Test
    void testGetFilteredMatchingRoleAssignmentsReturnsMatchingRoleAssignments() {
        FilteredRoleAssignments filteredRoleAssignments = new FilteredRoleAssignments();
        filteredRoleAssignments.addFilterMatchingResult(createRoleAssignmentFilteringResultAllMatchersMatch());
        filteredRoleAssignments.addFilterMatchingResult(createRoleAssignmentFilteringResultAllMatchersMatch());

        List<RoleAssignment> roleAssignments = filteredRoleAssignments.getFilteredMatchingRoleAssignments();

        assertEquals(2, roleAssignments.size());
    }

    private RoleAssignmentFilteringResult createRoleAssignmentFilteringResultAllMatchersMatch() {
        Map<String, Boolean> filteringResults = new HashMap<>();

        IntStream.range(0, 5).forEach(x -> filteringResults.put("roleMatcher" + x, Boolean.TRUE));

        return new RoleAssignmentFilteringResult(
            RoleAssignment.builder().id(UUID.randomUUID().toString()).build(),
            filteringResults);
    }

    @DisplayName("Check RoleAssignments with STANDARD grant type that failed filtering on "
        + "Region or Location are returned as expected")
    @Test
    void testGetFilteredStandardRoleAssignmentsFailedOnRegionOrBaseLocationMatcher() {

        FilteredRoleAssignments filteredRoleAssignments = new FilteredRoleAssignments();
        filteredRoleAssignments.addFilterMatchingResult(createRoleAssignmentFilteringResultAllMatchersMatch());
        filteredRoleAssignments.addFilterMatchingResult(createRoleAssignmentFilteringResultAllMatchersMatch());
        filteredRoleAssignments.addFilterMatchingResult(createRoleAssignmentFilteringResultAllMatchersMatch());

        List<String> roleAssignmentIds = List.of("2", "4");

        filteredRoleAssignments.addFilterMatchingResult(
            createFailedLocationAndRegionMatcherResultsForRoleAssignment(
                RoleAssignment.builder()
                    .id(roleAssignmentIds.get(0))
                    .grantType(GrantType.STANDARD.name())
                    .build()
            ));

        filteredRoleAssignments.addFilterMatchingResult(
            createFailedLocationAndRegionMatcherResultsForRoleAssignment(
                RoleAssignment.builder()
                    .id(roleAssignmentIds.get(1))
                    .grantType(GrantType.STANDARD.name())
                    .build()
                ));


        filteredRoleAssignments.addFilterMatchingResult(
            createFailedLocationAndRegionMatcherResultsForRoleAssignment(
                RoleAssignment.builder()
                    .id("6")
                    .grantType(GrantType.BASIC.name())
                    .build()
                ));

        filteredRoleAssignments.addFilterMatchingResult(
            createFailedLocationAndRegionMatcherResultsForRoleAssignment(
                RoleAssignment.builder()
                    .id("8")
                    .grantType(GrantType.EXCLUDED.name())
                    .build()
                ));

        // expect 3 results where RoleAssignment filtering matchers have all returned true

        assertEquals(3, filteredRoleAssignments.getFilteredMatchingRoleAssignments().size());

        List<RoleAssignment> filteredMatchingRoleAssignments =
            filteredRoleAssignments.getFilteredRoleAssignmentsFailedOnRegionOrBaseLocationMatcher();

        // expect 2 results where RoleAssignment filtering matchers have all passed apart from Region or Location
        // matchers, for RoleAssignment with STANDARD grant type
        assertEquals(2, filteredMatchingRoleAssignments.size());


        assertTrue(filteredMatchingRoleAssignments.stream().map(RoleAssignment::getId).collect(Collectors.toList())
            .containsAll(roleAssignmentIds));
    }

    @DisplayName("Check RoleAssignments with a STANDARD grant type that failed filtering on "
        + "Region, Location and any other matcher are not returned")
    @Test
    void testGetFilteredStandardRoleAssignmentsFailedOnRegionBaseLocationAndAnotherMatcher() {
        Map<String, Boolean> filteringResults = new HashMap<>();
        filteringResults.put("AnotherRoleMatcher", Boolean.FALSE);
        filteringResults.put("RegionMatcher", Boolean.FALSE);
        filteringResults.put("LocationMatcher", Boolean.FALSE);

        FilteredRoleAssignments filteredRoleAssignments = new FilteredRoleAssignments();

        filteredRoleAssignments.addFilterMatchingResult(
            new RoleAssignmentFilteringResult(
                RoleAssignment.builder().grantType(GrantType.STANDARD.name()).build(),
                filteringResults));

        // expect no results where RoleAssignment filtering matchers have all passed apart from Region or Location and
        // another matchers, for RoleAssignment with STANDARD grant type
        assertTrue(filteredRoleAssignments.getFilteredRoleAssignmentsFailedOnRegionOrBaseLocationMatcher().isEmpty());
    }

    @Test
    void testGetFilteredBasicRoleAssignmentsFailedOnRegionOrBaseLocationMatcherReturnsNoResults() {

        FilteredRoleAssignments filteredRoleAssignments = new FilteredRoleAssignments();

        filteredRoleAssignments.addFilterMatchingResult(
            createFailedLocationAndRegionMatcherResultsForRoleAssignment(
                RoleAssignment.builder()
                    .grantType(GrantType.BASIC.name())
                    .build()
                ));

        filteredRoleAssignments.addFilterMatchingResult(
            createFailedLocationAndRegionMatcherResultsForRoleAssignment(
                RoleAssignment.builder()
                    .grantType(GrantType.BASIC.name())
                    .build()
                ));

        filteredRoleAssignments.addFilterMatchingResult(
            createFailedLocationAndRegionMatcherResultsForRoleAssignment(RoleAssignment.builder()
                    .grantType(GrantType.EXCLUDED.name())
                    .build()
            ));

        assertTrue(filteredRoleAssignments.getFilteredMatchingRoleAssignments().isEmpty());

        assertTrue(filteredRoleAssignments.getFilteredRoleAssignmentsFailedOnRegionOrBaseLocationMatcher().isEmpty());
    }

    private RoleAssignmentFilteringResult createFailedLocationAndRegionMatcherResultsForRoleAssignment(
        RoleAssignment roleAssignment) {

        Map<String, Boolean> filteringResults = new HashMap<>();
        IntStream.range(0, 3).forEach(x -> filteringResults.put("roleMatcher" + x, Boolean.TRUE));

        filteringResults.put("RegionMatcher", Boolean.FALSE);
        filteringResults.put("LocationMatcher", Boolean.FALSE);

        return new RoleAssignmentFilteringResult(roleAssignment, filteringResults);
    }
}
