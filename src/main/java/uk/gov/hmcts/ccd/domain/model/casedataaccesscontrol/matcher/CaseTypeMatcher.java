package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

@Slf4j
@Component
public class CaseTypeMatcher implements RoleAttributeMatcher {

    @Override
    public MatcherType getType() {
        return MatcherType.CASETYPE;
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        return matchCaseType(roleAssignment,
            "Matching role assignment case type {} with case details case type {} for role assignment {}",
            caseDetails.getCaseTypeId(),
            "Role assignment case type {} and case details case type {} match {}");
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseTypeDefinition caseTypeDefinition) {

        return matchCaseType(roleAssignment,
            "Matching role assignment case type {} with case type {} for role assignment {}",
            caseTypeDefinition.getId(),
            "Role assignment case type {} and case type {} match {}");
    }

    private boolean matchCaseType(RoleAssignment roleAssignment,
                               String logMessage, String caseTypeId, String logMatchedMessage) {
        log.debug(logMessage,
            roleAssignment.getAttributes().getCaseType(),
            caseTypeId,
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment.getAttributes().getCaseType(), caseTypeId);
        log.debug(logMatchedMessage,
            roleAssignment.getAttributes().getCaseType(),
            caseTypeId,
            matched);
        return matched;
    }
}
