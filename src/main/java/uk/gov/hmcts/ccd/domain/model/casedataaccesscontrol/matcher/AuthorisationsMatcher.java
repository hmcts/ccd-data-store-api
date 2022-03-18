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

import java.util.List;
import java.util.Map;

import static java.lang.String.join;

@Slf4j
@Component
public class AuthorisationsMatcher implements RoleAttributeMatcher {

    private AuthorisationMapper authorisationMapper;

    @Autowired
    AuthorisationsMatcher(AuthorisationMapper authorisationMapper) {
        this.authorisationMapper = authorisationMapper;
    }

    @Override
    public MatcherType getType() {
        return MatcherType.AUTHORISATION;
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        Map<String, List<RoleToAccessProfileDefinition>> roleToAccessProfileDefinitionMap =
            authorisationMapper.toRoleNameAsKeyMap(caseDetails.getCaseTypeId());

        return matchAuthorisations(roleAssignment, roleToAccessProfileDefinitionMap);
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseTypeDefinition caseTypeDefinition) {
        Map<String, List<RoleToAccessProfileDefinition>> roleToAccessProfileDefinitionMap =
            authorisationMapper.toRoleNameAsKeyMap(caseTypeDefinition);
        return matchAuthorisations(roleAssignment, roleToAccessProfileDefinitionMap);
    }

    private boolean matchAuthorisations(RoleAssignment roleAssignment,
                                        Map<String, List<RoleToAccessProfileDefinition>>
                                            roleToAccessProfileDefinitionMap) {

        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = roleToAccessProfileDefinitionMap
            .get(roleAssignment.getRoleName());
        List<String> roleAssignmentAuthorisations = roleAssignment.getAuthorisations();

        boolean emptyRoleAssignmentAuthorisations = CollectionUtils.isEmpty(roleAssignmentAuthorisations);
        if (!emptyRoleAssignmentAuthorisations && roleToAccessProfileDefinitions != null
            && !roleToAccessProfileDefinitions.isEmpty()) {

            return roleToAccessProfileDefinitions.stream().anyMatch(definition -> {
                    List<String> definitionAuthorisations = definition.getAuthorisationList();
                    Boolean match = authorisationMapper
                        .authorisationsAllowMappingToAccessProfiles(definitionAuthorisations,
                            roleAssignmentAuthorisations);
                    log.debug("Role Assignment id: {}, roleName: {} - Matching Authorisations to {} from "
                            + "Access Profiles: {} with role assignment Authorisations: {}",
                        roleAssignment.getId(),
                        roleAssignment.getRoleName(),
                        match,
                        join(",", (definitionAuthorisations)), join(",", roleAssignmentAuthorisations));
                    return match;
                }
            );
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
