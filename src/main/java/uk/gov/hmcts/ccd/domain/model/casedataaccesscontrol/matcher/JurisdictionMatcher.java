package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

@Slf4j
@Component
public class JurisdictionMatcher implements RoleAttributeMatcher {

    @Override
    public MatcherType getType() {
        return MatcherType.JURISDICTION;
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        return matchJurisdiction(roleAssignment, caseDetails.getJurisdiction());
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseTypeDefinition caseTypeDefinition) {
        return matchJurisdiction(roleAssignment, caseTypeDefinition.getJurisdictionId());
    }

    private boolean matchJurisdiction(RoleAssignment roleAssignment, String jurisdictionId) {
        Optional<String> roleJurisdiction = roleAssignment.getAttributes().getJurisdiction();
        log.debug("Matching role assignment jurisdiction {} with case definition jurisdiction {}"
                + " for role assignment {}",
            roleJurisdiction,
            jurisdictionId,
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleJurisdiction, jurisdictionId);
        log.debug("Role assignment jurisdiction {} and case definition jurisdiction {} match {}",
            roleJurisdiction,
            jurisdictionId,
            matched);
        return matched;
    }
}
