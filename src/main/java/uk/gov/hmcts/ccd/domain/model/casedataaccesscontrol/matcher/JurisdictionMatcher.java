package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

@Slf4j
@Component
public class JurisdictionMatcher implements RoleAttributeMatcher {

    private final List<String> crossJurisdictionalRoles;

    @Autowired
    public JurisdictionMatcher(ApplicationParams applicationParams) {
        this.crossJurisdictionalRoles = applicationParams.getCcdAccessControlCrossJurisdictionRoles();
    }

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
        Optional<String> roleJurisdiction = Objects.requireNonNullElse(
            roleAssignment.getAttributes().getJurisdiction(), Optional.empty());
        log.debug("Matching role assignment jurisdiction {} with case definition jurisdiction {}"
                + " for role assignment {}",
            roleJurisdiction,
            jurisdictionId,
            roleAssignment.getId());

        // Defense-in-depth: fail-closed for null/absent jurisdiction attributes.
        // A role assignment with no jurisdiction must be an explicitly designated
        // cross-jurisdictional role; all other roles are denied cross-jurisdiction access.
        if (roleJurisdiction.isEmpty()) {
            boolean isCrossJurisdiction = crossJurisdictionalRoles.contains(roleAssignment.getRoleName());
            log.debug("Role assignment jurisdiction is null/absent for role {}. "
                    + "Cross-jurisdictional access granted: {}",
                roleAssignment.getRoleName(),
                isCrossJurisdiction);
            return isCrossJurisdiction;
        }

        boolean matched = isValuesMatching(roleJurisdiction, jurisdictionId);
        log.debug("Role assignment jurisdiction {} and case definition jurisdiction {} match {}",
            roleJurisdiction,
            jurisdictionId,
            matched);
        return matched;
    }
}
