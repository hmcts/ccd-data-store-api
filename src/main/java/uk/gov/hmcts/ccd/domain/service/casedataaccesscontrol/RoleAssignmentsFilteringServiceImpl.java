package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.RoleAttributeMatcher;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
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

        return filterMatchingRoleAssignments(roleAssignments,
            (matcher, roleAssignment) -> matcher.matchAttribute(roleAssignment, caseDetails));
    }

    @Override
    public List<RoleAssignment>  filter(RoleAssignments roleAssignments,
                                                      CaseTypeDefinition caseTypeDefinition) {
        log.info("Filter role assignments for case type {}", caseTypeDefinition.getName());

        return filterMatchingRoleAssignments(roleAssignments,
            (matcher, roleAssignment) -> matcher.matchAttribute(roleAssignment, caseTypeDefinition));

    }


    private List<RoleAssignment> filterMatchingRoleAssignments(
        RoleAssignments roleAssignments,
        BiPredicate<RoleAttributeMatcher, RoleAssignment> hasMatch) {
        return roleAssignments
            .getRoleAssignments()
            .stream()
            .filter(roleAssignment -> roleAttributeMatchers
                .stream()
                .allMatch(matcher -> hasMatch.test(matcher, roleAssignment)))
            .collect(Collectors.toList());
    }

}
