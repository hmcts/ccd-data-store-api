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
public class JurisdictionMatcher implements RoleAttributeMatcher {

    @Override
    public void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPai, CaseDetails caseDetails) {
        RoleAssignment roleAssignment = resultPai.getLeft();
        log.debug("Matching role assignment jurisdiction {} with case details jurisdiction {}"
                + " for role assignment {}",
            roleAssignment.getAttributes().getCaseId(),
            caseDetails.getJurisdiction(),
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment
                .getAttributes().getJurisdiction(),
            caseDetails.getJurisdiction());
        resultPai.getRight().setJurisdictionMatched(matched);
        log.debug("Role assignment jurisdiction {} and case details jurisdiction {} match {}",
            roleAssignment.getAttributes().getCaseId(),
            caseDetails.getJurisdiction(),
            matched);
    }

    @Override
    public void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPair,
                               CaseTypeDefinition caseTypeDefinition) {
        RoleAssignment roleAssignment = resultPair.getLeft();
        log.debug("Matching role assignment jurisdiction {} with case type definition jurisdiction {}"
                + " for role assignment {}",
            roleAssignment.getAttributes().getCaseId(),
            caseTypeDefinition.getJurisdictionId(),
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment
                .getAttributes().getJurisdiction(),
            caseTypeDefinition.getJurisdictionId());
        resultPair.getRight().setJurisdictionMatched(matched);
        log.debug("Role assignment jurisdiction {} and case type definition jurisdiction {} match {}",
            roleAssignment.getAttributes().getCaseId(),
            caseTypeDefinition.getJurisdictionId(),
            matched);
    }
}
