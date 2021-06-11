package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.AuthorisationMapper;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

@Slf4j
@Component
public class AuthorisationsMatcher implements RoleAttributeMatcher, AuthorisationMapper {

    private CaseTypeService caseTypeService;

    @Autowired
    AuthorisationsMatcher(CaseTypeService caseTypeService) {
        this.caseTypeService = caseTypeService;
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        return matchAuthorisations(roleAssignment, caseTypeService.getCaseType(caseDetails.getCaseTypeId()));
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseTypeDefinition caseTypeDefinition) {
        return matchAuthorisations(roleAssignment, caseTypeDefinition);
    }

    private boolean matchAuthorisations(RoleAssignment roleAssignment, CaseTypeDefinition caseTypeDefinition) {
        Map<String, RoleToAccessProfileDefinition> roleToAccessProfileDefinitionMap =
            toRoleNameAsKeyMap(caseTypeDefinition.getRoleToAccessProfiles());

        RoleToAccessProfileDefinition roleToAccessProfileDefinition = roleToAccessProfileDefinitionMap
            .get(roleAssignment.getRoleName());
        List<String> roleAssignmentAuthorisations = roleAssignment.getAuthorisations();
        if (roleAssignmentAuthorisations != null && roleAssignmentAuthorisations.size() > 0) {
            if (roleToAccessProfileDefinition != null && !roleToAccessProfileDefinition.getDisabled()) {
                List<String> definitionAuthorisations = roleToAccessProfileDefinition.getAuthorisationList();

                if (authorisationsAllowMappingToAccessProfiles(definitionAuthorisations,
                    roleAssignmentAuthorisations)) {
                    return true;
                }
            }
        }
        return false;
    }
}
