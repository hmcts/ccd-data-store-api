package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

@Slf4j
@Component
public class GrantTypeMatcher implements RoleAttributeMatcher {

    @Override
    public MatcherType getType() {
        return MatcherType.GRANTTYPE;
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseTypeDefinition caseTypeDefinition) {
        boolean matchedGrantType = !GrantType.EXCLUDED.name().equals(roleAssignment.getGrantType());
        log.debug("Role Assignment id: {}, roleName: {} - Matching GrantType to {}",
            roleAssignment.getId(),
            roleAssignment.getRoleName(),
            matchedGrantType);

        return matchedGrantType;
    }
}
