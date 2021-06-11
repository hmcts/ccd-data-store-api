package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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
public class AuthorisationsMatcher implements RoleAttributeMatcher {

    private CaseTypeService caseTypeService;

    private AuthorisationMapper authorisationMapper;

    @Autowired
    AuthorisationsMatcher(CaseTypeService caseTypeService,
                          AuthorisationMapper authorisationMapper) {
        this.caseTypeService = caseTypeService;
        this.authorisationMapper = authorisationMapper;
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
            authorisationMapper.toRoleNameAsKeyMap(caseTypeDefinition.getRoleToAccessProfiles());

        RoleToAccessProfileDefinition roleToAccessProfileDefinition = roleToAccessProfileDefinitionMap
            .get(roleAssignment.getRoleName());
        List<String> roleAssignmentAuthorisations = roleAssignment.getAuthorisations();

        if (!CollectionUtils.isEmpty(roleAssignmentAuthorisations)
            && roleToAccessProfileDefinition != null
            && !roleToAccessProfileDefinition.getDisabled()) {
            List<String> definitionAuthorisations = roleToAccessProfileDefinition.getAuthorisationList();
            return authorisationMapper.authorisationsAllowMappingToAccessProfiles(definitionAuthorisations,
                roleAssignmentAuthorisations);
        }
        return false;
    }
}
