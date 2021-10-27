package uk.gov.hmcts.ccd.domain.service;

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
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;

@Component
public class AuthorisationMapper {

    public Map<String, RoleToAccessProfileDefinition> toRoleNameAsKeyMap(
        List<RoleToAccessProfileDefinition> roleToAccessProfiles) {
        return roleToAccessProfiles
            .stream()
            .filter(e -> e.getRoleName() != null)
            .collect(Collectors.toMap(RoleToAccessProfileDefinition::getRoleName,
                Function.identity()));
    }

    public boolean authorisationsAllowMappingToAccessProfiles(List<String> authorisations,
                                                               List<String> roleAssignmentAuthorisations) {
        if (roleAssignmentAuthorisations != null
            && !authorisations.isEmpty()) {
            Collection<String> filterAuthorisations = CollectionUtils
                .intersection(roleAssignmentAuthorisations, authorisations);

            return !filterAuthorisations.isEmpty();
        }
        return authorisations.isEmpty();
    }

    public List<AccessProfile> createAccessProfiles(RoleAssignment roleAssignment,
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
        return BooleanUtils.isTrue(roleAssignment.getReadOnly())
            || BooleanUtils.isTrue(roleToAccessProfileDefinition.getReadOnly());
    }
}
