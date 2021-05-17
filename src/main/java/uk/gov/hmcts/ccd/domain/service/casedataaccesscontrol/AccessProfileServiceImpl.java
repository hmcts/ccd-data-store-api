package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

@Component
public class AccessProfileServiceImpl implements AccessProfileService, AccessControl {

    @Override
    public List<AccessProfile> generateAccessProfiles(RoleAssignmentFilteringResult filteringResults,
                                                      List<RoleToAccessProfileDefinition> roleToAccessProfiles) {

        List<AccessProfile> accessProfiles = new ArrayList<>();
        Map<String, RoleToAccessProfileDefinition> roleToAccessProfileDefinitionMap =
            toRoleNameAsKeyMap(roleToAccessProfiles);

        for (RoleAssignment roleAssignment : filteringResults.getRoleAssignments()) {

            RoleToAccessProfileDefinition roleToAccessProfileDefinition =
                roleToAccessProfileDefinitionMap.get(roleAssignment.getRoleName());

            if (roleToAccessProfileDefinition != null && !roleToAccessProfileDefinition.getDisabled()) {
                List<String> definitionAuthorisations = roleToAccessProfileDefinition.getAuthorisationList();
                List<String> roleAssignmentAuthorisations = roleAssignment.getAuthorisations();

                if (authorisationsAllowMappingToAccessProfiles(definitionAuthorisations,
                    roleAssignmentAuthorisations)) {
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
            .collect(Collectors.toMap(RoleToAccessProfileDefinition::getRoleName,
                                      Function.identity()));
    }

    private boolean authorisationsAllowMappingToAccessProfiles(List<String> authorisations,
                                                               List<String> roleAssignmentAuthorisations) {
        if (roleAssignmentAuthorisations != null
            && !authorisations.isEmpty()) {
            Collection<String> filterAuthorisations = CollectionUtils
                .intersection(roleAssignmentAuthorisations, authorisations);

            return !filterAuthorisations.isEmpty();
        }
        return authorisations.isEmpty();
    }

    private List<AccessProfile> createAccessProfiles(RoleAssignment roleAssignment,
                                                     RoleToAccessProfileDefinition roleToAccessProfileDefinition) {
        List<String> accessProfileList = roleToAccessProfileDefinition.getAccessProfileList();
        return accessProfileList
            .stream()
            .map(accessProfileValue -> AccessProfile.builder()
                .accessProfile(accessProfileValue)
                .securityClassification(roleAssignment.getClassification())
                .readOnly(readOnly(roleAssignment, roleToAccessProfileDefinition))
                .build()).collect(Collectors.toList());
    }

    private Boolean readOnly(RoleAssignment roleAssignment,
                             RoleToAccessProfileDefinition roleToAccessProfileDefinition) {
        return BooleanUtils.isTrue(roleAssignment.getReadOnly()) ||
            BooleanUtils.isTrue(roleToAccessProfileDefinition.getReadOnly());
    }
}
