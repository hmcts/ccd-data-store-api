package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;

@Component
public class RoleAssignmentToAccessProfileMapperImpl implements RoleAssignmentToAccessProfileMapper {

    @Override
    public List<AccessProfile> toAccessProfiles(RoleAssignmentFilteringResult filteringResult,
                                                CaseTypeDefinition caseTypeDefinition) {
        List<RoleAssignment> roleAssignments = extractRoleAssignments(filteringResult);

        if (hasGrantTypeExcluded(roleAssignments)) {
            roleAssignments = filterRoleAssignments(roleAssignments);
        }

        List<AccessProfile> accessProfiles = new ArrayList<>();
        for (RoleAssignment roleAssignment : roleAssignments) {
            for (RoleToAccessProfileDefinition roleToAccessProfile : caseTypeDefinition.getRoleToAccessProfiles()) {
                if (validRoleAssignments(roleToAccessProfile, roleAssignment)) {

                    List<String> authorisationsList = Arrays.asList(roleToAccessProfile.getAuthorisation().split(","));

                    List<String> roleAssignmentAuthorisation = roleAssignment.getAuthorisations();

                    if (roleAssignmentAuthorisation != null && roleAssignmentAuthorisation.size() > 0) {
                        for (String authorisation : authorisationsList) {
                            if (roleAssignmentAuthorisation.contains(authorisation)) {
                                accessProfiles.add(createAccessProfile(roleAssignment, roleToAccessProfile));
                            }
                        }
                    }
                }
            }
        }
        return accessProfiles;
    }

    private boolean validRoleAssignments(RoleToAccessProfileDefinition roleToAccessProfileDefinition,
                                         RoleAssignment roleAssignment) {
        return !roleToAccessProfileDefinition.getDisabled()
            && roleAssignment.getRoleName().equals(roleToAccessProfileDefinition.getRoleName())
            && !StringUtils.isEmpty(roleToAccessProfileDefinition.getAuthorisation());
    }

    private List<RoleAssignment> extractRoleAssignments(RoleAssignmentFilteringResult filteringResult) {
        return filteringResult.getRoleAssignmentRoleMatchingResults()
            .stream()
            .map(pair -> pair.getLeft())
            .collect(Collectors.toList());
    }

    private AccessProfile createAccessProfile(RoleAssignment roleAssignment,
                                              RoleToAccessProfileDefinition roleToAccessProfile) {
        AccessProfile accessProfile = new AccessProfile();

        accessProfile.setReadOnly(roleToAccessProfile.getReadOnly() || roleAssignment.getReadOnly());
        accessProfile.setClassification(roleAssignment.getClassification());

        String caseTypeAccessProfiles = roleToAccessProfile.getAccessProfiles();
        accessProfile.setAccessProfiles(Arrays
            .asList(caseTypeAccessProfiles.split(",")));
        return accessProfile;
    }

    private boolean hasGrantTypeExcluded(List<RoleAssignment> roleAssignments) {
        return roleAssignments.stream()
            .anyMatch(roleAssignment -> roleAssignment.getGrantType().equals(GrantType.EXCLUDED.name()));
    }

    private List<RoleAssignment> filterRoleAssignments(List<RoleAssignment> roleAssignments) {
        return roleAssignments.stream()
            .filter(roleAssignment -> (roleAssignment.getGrantType().equals(GrantType.BASIC.name())
                || roleAssignment.getGrantType().equals(GrantType.STANDARD.name())))
            .collect(Collectors.toList());
    }
}
