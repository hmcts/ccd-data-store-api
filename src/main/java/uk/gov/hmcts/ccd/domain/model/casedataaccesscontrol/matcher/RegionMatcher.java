package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Slf4j
@Component
public class RegionMatcher implements RoleAttributeMatcher {

    @Override
    public void matchAttribute(RoleAssignmentFilteringResult result, CaseDetails caseDetails) {
        RoleAssignment roleAssignment = result.getRoleAssignment();
        String caseRegion = ""; // Get region from case Details
        log.debug("Match role assignment region {} and case details region {} for role assignment {}",
            roleAssignment.getAttributes().getRegion(),
            caseRegion,
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment.getAttributes().getRegion(), caseRegion);
        result.getRoleMatchingResult()
            .setRegionMatched(matched);

        log.debug("Role assignment region {} and case details region {} match {}",
            roleAssignment.getAttributes().getLocation(),
            caseRegion,
            matched);
    }
}
