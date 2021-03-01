package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.RoleAttributeMatcher;

@Slf4j
@Component
public class RoleAssignmentsFilteringServiceImpl implements RoleAssignmentsFilteringService, AccessControl {

    private List<RoleAttributeMatcher> roleAttributeMatchers;

    @Autowired
    public RoleAssignmentsFilteringServiceImpl(List<RoleAttributeMatcher> roleAttributeMatchers) {
        this.roleAttributeMatchers = roleAttributeMatchers;
    }

    @Override
    public List<RoleAssignmentFilteringResult> filter(RoleAssignments roleAssignments,
                                                      CaseDetails caseDetails) {
        log.info("Filter role assignments for case {}", caseDetails.getReference());

        List<RoleAssignmentFilteringResult> roleAssignmentFilterResults = roleAssignments
            .getRoleAssignments()
            .stream()
            .map(roleAssignment -> {
                RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignment,
                    new RoleMatchingResult());

                roleAttributeMatchers.forEach(matcher -> matcher.matchAttribute(result, caseDetails));
                return result;
            })
            .filter(result -> result.getRoleMatchingResult().matchedAllValues())
            .collect(Collectors.toList());

        return roleAssignmentFilterResults;
    }

    @Override
    public List<RoleAssignmentFilteringResult> filter(RoleAssignments roleAssignments,
                                                      CaseDataContent caseDataContent) {
        log.info("Filter role assignments for case event {}", caseDataContent.getEvent().getEventId());

        List<RoleAssignmentFilteringResult> roleAssignmentList = roleAssignments.getRoleAssignments().stream()
            .map(roleAssignment -> new RoleAssignmentFilteringResult(roleAssignment, new RoleMatchingResult()))
            .collect(Collectors.toList());
        return roleAssignmentList;
    }

    @Override
    public List<RoleAssignmentFilteringResult> filter(RoleAssignments roleAssignments,
                                                      CaseTypeDefinition caseTypeDefinition) {
        log.info("Filter role assignments for case type {}", caseTypeDefinition.getName());

        List<RoleAssignmentFilteringResult> roleAssignmentList = roleAssignments.getRoleAssignments().stream()
            .map(roleAssignment -> new RoleAssignmentFilteringResult(roleAssignment, new RoleMatchingResult()))
            .collect(Collectors.toList());
        return roleAssignmentList;
    }

}
