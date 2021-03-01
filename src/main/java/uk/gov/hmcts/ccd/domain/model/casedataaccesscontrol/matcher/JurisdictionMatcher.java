package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Slf4j
@Component
public class JurisdictionMatcher implements RoleAttributeMatcher {

    @Override
    public void matchAttribute(RoleAssignmentFilteringResult result, CaseDetails caseDetails) {
        RoleAssignment roleAssignment = result.getRoleAssignment();
        log.debug("Matching role assignment jurisdiction {} with case details jurisdiction {}"
                + " for role assignment {}",
            roleAssignment.getAttributes().getCaseId(),
            caseDetails.getJurisdiction(),
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment
                .getAttributes().getJurisdiction(),
            caseDetails.getJurisdiction());
        result.getRoleMatchingResult().setJurisdictionMatched(matched);
        log.debug("Role assignment jurisdiction {} and case details jurisdiction {} match {}",
            roleAssignment.getAttributes().getCaseId(),
            caseDetails.getJurisdiction(),
            matched);
    }
}
