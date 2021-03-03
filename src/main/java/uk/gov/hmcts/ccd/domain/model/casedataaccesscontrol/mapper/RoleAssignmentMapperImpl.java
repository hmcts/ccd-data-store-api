package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;

public class RoleAssignmentMapperImpl implements RoleAssignmentMapper {

    @Override
    public List<AccessProfile> map(List<RoleAssignment> roleAssignments,
                                   CaseTypeDefinition caseTypeDefinition) {

        if (hasGrantTypeExcluded(roleAssignments)) {
            roleAssignments = filterRoleAssignments(roleAssignments);
        }

        List<AccessProfile> accessProfiles = new ArrayList<>();
        for (RoleAssignment roleAssignment : roleAssignments) {
            for (RoleToAccessProfileDefinition roleToAccessProfile : caseTypeDefinition.getRoleToAccessProfiles()) {
                if (roleToAccessProfile.getRoleName().equals(roleAssignment.getRoleName())) {
                    String caseTypeAuthorisations = roleToAccessProfile.getAuthorisation();

                    if (!StringUtils.isEmpty(caseTypeAuthorisations)) {
                        List<String> authorisationsList = Arrays.asList(caseTypeAuthorisations.split(","));

                        List<String> roleAssignmentAuthorisation = roleAssignment.getAuthorisations();

                        for(String authorisation : authorisationsList) {
                            if (roleAssignmentAuthorisation.contains(authorisation)) {
                                AccessProfile accessProfile = createAccessProfile(roleAssignment,
                                    roleToAccessProfile);

                                accessProfiles.add(accessProfile);
                            }
                        }
                    }
                }
            }
        }
        return accessProfiles;
    }

    private AccessProfile createAccessProfile(RoleAssignment roleAssignment,
                                              RoleToAccessProfileDefinition roleToAccessProfile) {
        AccessProfile accessProfile = new AccessProfile();

        accessProfile.setReadOnly(roleToAccessProfile.getReadOnly());
        accessProfile.setClassification(roleAssignment.getClassification());

        String caseTypeAccessProfiles = roleToAccessProfile.getAccessProfiles();
        accessProfile.setAccessProfiles(Arrays
            .asList(caseTypeAccessProfiles.split("'")));
        return accessProfile;
    }

    private boolean hasGrantTypeExcluded(List<RoleAssignment> roleAssignments) {
        return roleAssignments.stream()
            .filter(roleAssignment -> roleAssignment.getGrantType().equals(GrantType.EXCLUDED.name()))
            .findFirst().isPresent();
    }

    private List<RoleAssignment> filterRoleAssignments(List<RoleAssignment> roleAssignments) {
         return roleAssignments.stream()
             .filter(roleAssignment -> !(roleAssignment.getGrantType().equals(GrantType.BASIC.name())
                 || roleAssignment.getGrantType().equals(GrantType.STANDARD.name())))
             .collect(Collectors.toList());
    }
}
