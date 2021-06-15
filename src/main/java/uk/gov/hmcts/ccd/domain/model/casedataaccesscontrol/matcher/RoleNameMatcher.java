package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;

import java.util.List;

@Slf4j
@Component
public class RoleNameMatcher implements RoleAttributeMatcher {

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        return true;
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseTypeDefinition caseTypeDefinition) {
        String roleName = roleAssignment.getRoleName();
        List<RoleToAccessProfileDefinition> roleToAccessProfiles = caseTypeDefinition.getRoleToAccessProfiles();

        return roleToAccessProfiles
            .stream()
            .filter(e -> e.getRoleName() != null)
            .map(RoleToAccessProfileDefinition::getRoleName)
            .anyMatch(e -> e.equals(roleName));
    }
}
