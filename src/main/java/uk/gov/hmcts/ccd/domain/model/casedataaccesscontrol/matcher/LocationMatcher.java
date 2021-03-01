package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Slf4j
@Component
public class LocationMatcher implements RoleAttributeMatcher {

    @Override
    public void matchAttribute(RoleAssignmentFilteringResult result, CaseDetails caseDetails) {
        RoleAssignment roleAssignment = result.getRoleAssignment();
        String caseLocation = ""; // Get location from case Details
        log.debug("Match role assignment location {} with case details location {} for role assignment {}",
            roleAssignment.getAttributes().getLocation(),
            "",
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment.getAttributes().getLocation(), caseLocation);
        result.getRoleMatchingResult()
            .setLocationMatched(matched);

        log.debug("Role assignment location {} and case details location {} match {}",
            roleAssignment.getAttributes().getLocation(),
            caseLocation,
            matched);
    }
}
