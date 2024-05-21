package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.MatcherType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.RoleAttributeMatcher;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

@Slf4j
@Component
public class RoleAssignmentsFilteringServiceImpl implements RoleAssignmentsFilteringService, AccessControl {

    private List<RoleAttributeMatcher> roleAttributeMatchers;

    @Autowired
    public RoleAssignmentsFilteringServiceImpl(List<RoleAttributeMatcher> roleAttributeMatchers) {
        this.roleAttributeMatchers = roleAttributeMatchers;
    }

    @Override
    public FilteredRoleAssignments filter(RoleAssignments roleAssignments,
                                          CaseDetails caseDetails) {
        log.debug("Filter role assignments for case {}", caseDetails.getReference());

        return filterMatchingRoleAssignments(roleAssignments,
            (matcher, roleAssignment) -> matcher.matchAttribute(roleAssignment, caseDetails));
    }

    @Override
    public FilteredRoleAssignments filter(RoleAssignments roleAssignments,
                                                      CaseTypeDefinition caseTypeDefinition) {
        log.debug("Filter role assignments for case type {}", caseTypeDefinition.getName());

        return filterMatchingRoleAssignments(roleAssignments,
            (matcher, roleAssignment) -> matcher.matchAttribute(roleAssignment, caseTypeDefinition));
    }

    @Override
    public FilteredRoleAssignments filter(RoleAssignments roleAssignments,
                                          CaseTypeDefinition caseTypeDefinition,
                                          List<MatcherType> excludeMatchers) {
        return filterMatchingRoleAssignments(roleAssignments,
            (matcher, roleAssignment) -> {
                if (!excludeMatchers.contains(matcher.getType())) {
                    return matcher.matchAttribute(roleAssignment, caseTypeDefinition);
                }
                return true;
            });
    }

    @Override
    public FilteredRoleAssignments filter(RoleAssignments roleAssignments,
                                          CaseDetails caseDetails,
                                          List<MatcherType> excludeMatchers) {
        return filterMatchingRoleAssignments(roleAssignments,
            (matcher, roleAssignment) -> {
                if (!excludeMatchers.contains(matcher.getType())) {
                    return matcher.matchAttribute(roleAssignment, caseDetails);
                }
                return true;
            });
    }

    private FilteredRoleAssignments filterMatchingRoleAssignments(
        RoleAssignments roleAssignments,
        BiPredicate<RoleAttributeMatcher, RoleAssignment> hasMatch) {

        FilteredRoleAssignments returnValue = new FilteredRoleAssignments();

        for (RoleAssignment roleAssignment: roleAssignments.getRoleAssignments()) {

            Map<String, Boolean> roleAttributeMatchersResults = new HashMap<>();
            for (RoleAttributeMatcher roleAttributeMatcher : roleAttributeMatchers) {
                boolean attributeMatched = hasMatch.test(roleAttributeMatcher, roleAssignment);
                roleAttributeMatchersResults.put(roleAttributeMatcher.getClass().getName(), attributeMatched);
            }

            returnValue.addFilterMatchingResult(new RoleAssignmentFilteringResult(roleAssignment,
                                                                                    roleAttributeMatchersResults));
        }

        return returnValue;
    }

}
