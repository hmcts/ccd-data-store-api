package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleMatchingResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

@Slf4j
@Component
public class CaseIdMatcher implements RoleAttributeMatcher {

    @Override
    public void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPair, CaseDetails caseDetails) {
        RoleAssignment roleAssignment = resultPair.getLeft();
        log.debug("Matching role assignment case id {} with case details case id {} for role assignment {}",
            roleAssignment.getAttributes().getCaseId(),
            caseDetails.getReferenceAsString(),
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment.getAttributes().getCaseId(),
            caseDetails.getReferenceAsString());
        resultPair.getRight().setCaseIdMatched(matched);
        log.debug("Role assignment case id {} and case details case id {} match {}",
            roleAssignment.getAttributes().getCaseId(),
            caseDetails.getReferenceAsString(),
            matched);
    }

    @Override
    public void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPair,
                               CaseTypeDefinition caseTypeDefinition) {

    }
}
