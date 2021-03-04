package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.RoleAttributeMatcher;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

@Slf4j
@Component
public class RoleAssignmentsFilteringServiceImpl implements RoleAssignmentsFilteringService, AccessControl {

    private List<RoleAttributeMatcher> roleAttributeMatchers;

    @Autowired
    public RoleAssignmentsFilteringServiceImpl(List<RoleAttributeMatcher> roleAttributeMatchers) {
        this.roleAttributeMatchers = roleAttributeMatchers;
    }

    @Override
    public RoleAssignmentFilteringResult filter(RoleAssignments roleAssignments,
                                                      CaseDetails caseDetails) {
        log.info("Filter role assignments for case {}", caseDetails.getReference());

        List<Pair<RoleAssignment, RoleMatchingResult>> roleAssignmentMatchPairs = roleAssignments
            .getRoleAssignments()
            .stream()
            .map(roleAssignment -> {
                Pair<RoleAssignment, RoleMatchingResult> resultPair = Pair.of(roleAssignment, new RoleMatchingResult());

                roleAttributeMatchers.forEach(matcher -> matcher.matchAttribute(resultPair, caseDetails));
                return resultPair;
            })
            .filter(resultPair -> resultPair.getRight().matchedAllValues())
            .collect(Collectors.toList());

        return new RoleAssignmentFilteringResult(roleAssignmentMatchPairs);
    }

    @Override
    public RoleAssignmentFilteringResult filter(RoleAssignments roleAssignments,
                                                      CaseDataContent caseDataContent) {
        log.info("Filter role assignments for case event {}", caseDataContent.getEvent().getEventId());

        List<Pair<RoleAssignment, RoleMatchingResult>> roleAssignmentMatchPairs = roleAssignments
            .getRoleAssignments().stream()
            .map(roleAssignment -> Pair.of(roleAssignment, new RoleMatchingResult()))
            .collect(Collectors.toList());
        return new RoleAssignmentFilteringResult(roleAssignmentMatchPairs);
    }

    @Override
    public RoleAssignmentFilteringResult filter(RoleAssignments roleAssignments,
                                                      CaseTypeDefinition caseTypeDefinition) {
        log.info("Filter role assignments for case type {}", caseTypeDefinition.getName());

        List<Pair<RoleAssignment, RoleMatchingResult>> roleAssignmentMatchPairs = roleAssignments
            .getRoleAssignments().stream()
            .map(roleAssignment -> Pair.of(roleAssignment, new RoleMatchingResult()))
            .collect(Collectors.toList());
        return new RoleAssignmentFilteringResult(roleAssignmentMatchPairs);
    }

}
