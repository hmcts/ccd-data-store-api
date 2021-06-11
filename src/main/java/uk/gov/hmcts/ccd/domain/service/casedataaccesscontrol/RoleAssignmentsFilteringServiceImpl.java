package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
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
    public List<RoleAssignment>  filter(RoleAssignments roleAssignments,
                                                CaseDetails caseDetails) {
        log.info("Filter role assignments for case {}", caseDetails.getReference());

        return roleAssignments
            .getRoleAssignments()
            .stream()
            .filter(roleAssignment -> roleAttributeMatchers
                .stream()
                .map(matcher -> matcher.matchAttribute(roleAssignment, caseDetails))
                .allMatch(matched -> matched == true))
            .collect(Collectors.toList());
    }

    @Override
    public List<RoleAssignment> filter(RoleAssignments roleAssignments,
                                       CaseDataContent caseDataContent) {
        log.info("Filter role assignments for case event {}", caseDataContent.getEvent().getEventId());

        return roleAssignments
            .getRoleAssignments()
            .stream()
            .filter(roleAssignment -> roleAttributeMatchers
                .stream()
                .map(matcher -> matcher.matchAttribute(roleAssignment, (CaseTypeDefinition) null))
                .allMatch(matched -> matched == true))
            .collect(Collectors.toList());
    }

    @Override
    public List<RoleAssignment>  filter(RoleAssignments roleAssignments,
                                                      CaseTypeDefinition caseTypeDefinition) {
        log.info("Filter role assignments for case type {}", caseTypeDefinition.getName());

        return roleAssignments
            .getRoleAssignments()
            .stream()
            .filter(roleAssignment -> roleAttributeMatchers
                .stream()
                .map(matcher -> matcher.matchAttribute(roleAssignment, caseTypeDefinition))
                .allMatch(matched -> matched == true))
            .collect(Collectors.toList());
    }

}
