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
public class CaseTypeMatcher implements RoleAttributeMatcher {

    @Override
    public void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPair, CaseDetails caseDetails) {
        matchCaseType(resultPair,
            "Matching role assignment case type {} with case details case type {} for role assignment {}",
            caseDetails.getCaseTypeId(),
            "Role assignment case type {} and case details case type {} match {}");
    }

    @Override
    public void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPair,
                               CaseTypeDefinition caseTypeDefinition) {
        matchCaseType(resultPair,
            "Matching role assignment case type {} with case type {} for role assignment {}",
            caseTypeDefinition.getId(),
            "Role assignment case type {} and case type {} match {}");
    }

    private void matchCaseType(Pair<RoleAssignment, RoleMatchingResult> resultPair,
                               String logMessage, String caseTypeId, String logMatchedMessage) {
        RoleAssignment roleAssignment = resultPair.getLeft();
        log.debug(logMessage,
            roleAssignment.getAttributes().getCaseTypeId(),
            caseTypeId,
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment.getAttributes().getCaseTypeId(), caseTypeId);
        resultPair.getRight().setCaseIdMatched(matched);
        log.debug(logMatchedMessage,
            roleAssignment.getAttributes().getCaseTypeId(),
            caseTypeId,
            matched);
    }
}
