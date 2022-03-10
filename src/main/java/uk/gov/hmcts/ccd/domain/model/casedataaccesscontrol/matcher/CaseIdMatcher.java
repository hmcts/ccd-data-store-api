package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Slf4j
@Component
public class CaseIdMatcher implements RoleAttributeMatcher {

    @Override
    public MatcherType getType() {
        return MatcherType.CASEID;
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        log.debug("Matching role assignment case id {} with case details case id {} for role assignment {}",
            roleAssignment.getAttributes().getCaseId(),
            caseDetails.getReferenceAsString(),
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment.getAttributes().getCaseId(),
            caseDetails.getReferenceAsString());
        log.debug("Role assignment case id {} and case details case id {} match {}",
            roleAssignment.getAttributes().getCaseId(),
            caseDetails.getReferenceAsString(),
            matched);
        return matched;
    }
}
