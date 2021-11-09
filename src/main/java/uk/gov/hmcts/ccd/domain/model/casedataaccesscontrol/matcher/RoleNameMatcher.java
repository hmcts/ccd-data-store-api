package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;

import java.util.List;

@Slf4j
@Component
public class RoleNameMatcher implements RoleAttributeMatcher {

    @Override
    public MatcherType getType() {
        return MatcherType.ROLENAME;
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseTypeDefinition caseTypeDefinition) {
        String roleName = roleAssignment.getRoleName();
        List<RoleToAccessProfileDefinition> roleToAccessProfiles = caseTypeDefinition.getRoleToAccessProfiles();
        boolean matchedRoleName = roleToAccessProfiles == null || roleToAccessProfiles.isEmpty() || roleToAccessProfiles
            .stream()
            .filter(e -> e.getRoleName() != null)
            .map(RoleToAccessProfileDefinition::getRoleName)
            .anyMatch(e -> e.equals(roleName));
        log.debug("Role Assignment id: {}, roleName: {} - Matching RoleName to {}",
            roleAssignment.getId(),
            roleAssignment.getRoleName(),
            matchedRoleName);

        return matchedRoleName;
    }
}
