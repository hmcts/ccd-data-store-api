package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AccessProfileServiceImpl implements AccessProfileService, AccessControl {

    @Override
    @SuppressWarnings("checkstyle:LineLength")
    public List<AccessProfile> generateAccessProfiles(RoleAssignmentFilteringResult filteringResults,
                                                      List<RoleToAccessProfileDefinition> roleToAccessProfileMappings) {

        List<AccessProfile> accessProfiles = new ArrayList<>();
        Map<String, RoleToAccessProfileDefinition> roleToAccessProfileDefinitionMap =
            toRoleNameAsKeyMap(roleToAccessProfileMappings);

        for (RoleAssignment roleAssignment : filteringResults.getRoleAssignments()) {

            RoleToAccessProfileDefinition roleToAccessProfileDefinition =
                roleToAccessProfileDefinitionMap.get(roleAssignment.getRoleName());

            if (roleToAccessProfileDefinition != null && !roleToAccessProfileDefinition.isDisabled()) {
                List<String> authorisations = roleToAccessProfileDefinition.getAuthorisationList();
                List<String> roleAssignmentAuthorisations = roleAssignment.getAuthorisations();

                if (authorisationsAllowMappingToAccessProfiles(authorisations, roleAssignmentAuthorisations)) {
                    accessProfiles.addAll(createAccessProfiles(roleAssignment, roleToAccessProfileDefinition));
                }
            }
        }
        return accessProfiles;
    }

    private Map<String, RoleToAccessProfileDefinition> toRoleNameAsKeyMap(
        List<RoleToAccessProfileDefinition> roleToAccessProfiles) {
        return roleToAccessProfiles
            .stream()
            .filter(e -> e.getRoleName() != null)
            .collect(Collectors.toMap(RoleToAccessProfileDefinition::getRoleName,
                                      Function.identity()));
    }

    private boolean authorisationsAllowMappingToAccessProfiles(List<String> authorisations,
                                                               List<String> roleAssignmentAuthorisations) {
        if (roleAssignmentAuthorisations != null
            && authorisations.size() > 0) {
            Collection<String> filterAuthorisations = CollectionUtils
                .intersection(roleAssignmentAuthorisations, authorisations);

            return filterAuthorisations.size() > 0;
        }
        return authorisations.size() == 0;
    }

    private List<AccessProfile> createAccessProfiles(RoleAssignment roleAssignment,
                                                     RoleToAccessProfileDefinition roleToAccessProfileDefinition) {
        List<String> accessProfileList = roleToAccessProfileDefinition.getAccessProfileList();
        return accessProfileList
            .stream()
            .map(accessProfileValue -> {
                AccessProfile accessProfile = new AccessProfile();

                accessProfile.setReadOnly(roleToAccessProfileDefinition.isReadOnly()
                    || (roleAssignment.getReadOnly() != null && roleAssignment.getReadOnly()));
                accessProfile.setClassification(roleAssignment.getClassification());
                accessProfile.setAccessProfile(accessProfileValue);
                return accessProfile;
            }).collect(Collectors.toList());
    }
}
