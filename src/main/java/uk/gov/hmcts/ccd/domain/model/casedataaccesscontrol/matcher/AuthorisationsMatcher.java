package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

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

import java.util.List;
import java.util.Map;

import static java.lang.String.join;

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

        boolean emptyRoleAssignmentAuthorisations = CollectionUtils.isEmpty(roleAssignmentAuthorisations);
        if (!emptyRoleAssignmentAuthorisations && roleToAccessProfileDefinition != null) {
            List<String> definitionAuthorisations = roleToAccessProfileDefinition.getAuthorisationList();
            boolean match = authorisationMapper.authorisationsAllowMappingToAccessProfiles(definitionAuthorisations,
                roleAssignmentAuthorisations);
            log.debug("Role Assignment id: {}, roleName: {} - Matching Authorisations to {} from Access Profiles: {}"
                    + " with role assignment Authorisations: {}",
                roleAssignment.getId(),
                roleAssignment.getRoleName(),
                match,
                join(",", definitionAuthorisations),
                join(",", roleAssignmentAuthorisations));
            return match;
        }
        log.debug("Role Assignment id: {}, roleName: {} - Matching Authorisations to {}"
                + " with role assignment Authorisations: {}",
            roleAssignment.getId(),
            roleAssignment.getRoleName(),
            emptyRoleAssignmentAuthorisations,
            roleAssignmentAuthorisations != null ? join(",", roleAssignmentAuthorisations) : null);
        return emptyRoleAssignmentAuthorisations;
    }
}
