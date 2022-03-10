package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.AuthorisationMapper;

@Component
public class AccessProfileServiceImpl implements AccessProfileService, AccessControl {

    private AuthorisationMapper authorisationMapper;

    @Autowired
    public AccessProfileServiceImpl(AuthorisationMapper authorisationMapper) {
        this.authorisationMapper = authorisationMapper;
    }

    @Override
    @SuppressWarnings("checkstyle:LineLength")
    public List<AccessProfile> generateAccessProfiles(List<RoleAssignment> filteredRoleAssignments,
                                                      List<RoleToAccessProfileDefinition> roleToAccessProfilesMappings) {

        // TODO: Think about improving this, as most of this logic is already done in AuthorisationsMatcher
        List<AccessProfile> accessProfiles = new ArrayList<>();
        Map<String, List<RoleToAccessProfileDefinition>> roleToAccessProfileDefinitionMap =
            authorisationMapper.toRoleNameAsKeyMap(roleToAccessProfilesMappings);

        for (RoleAssignment roleAssignment : filteredRoleAssignments) {

            List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions =
                roleToAccessProfileDefinitionMap.get(roleAssignment.getRoleName());

            if (roleToAccessProfileDefinitions != null) {
                for (RoleToAccessProfileDefinition roleToAccessProfileDefinition : roleToAccessProfileDefinitions) {

                    if (roleToAccessProfileDefinition != null && !roleToAccessProfileDefinition.getDisabled()) {
                        List<String> definitionAuthorisations = roleToAccessProfileDefinition.getAuthorisationList();
                        List<String> roleAssignmentAuthorisations = roleAssignment.getAuthorisations();

                        if (authorisationMapper.authorisationsAllowMappingToAccessProfiles(definitionAuthorisations,
                            roleAssignmentAuthorisations)) {
                            accessProfiles.addAll(authorisationMapper
                                .createAccessProfiles(roleAssignment, roleToAccessProfileDefinition));
                        }
                    }
                }
            }
        }
        return accessProfiles;
    }
}
