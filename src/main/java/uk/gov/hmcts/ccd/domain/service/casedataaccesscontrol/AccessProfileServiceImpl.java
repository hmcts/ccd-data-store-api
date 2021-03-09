package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

@Component
public class AccessProfileServiceImpl implements AccessProfileService, AccessControl {

    @Override
    public List<AccessProfile> generateAccessProfiles(RoleAssignmentFilteringResult filteringResults,
                                                      CaseTypeDefinition caseTypeDefinition) {

        List<AccessProfile> accessProfiles = new ArrayList<>();

        for (RoleAssignment roleAssignment : filteringResults.getRoleAssignments()) {
            List<String> roleAssignmentAuthorisations = roleAssignment.getAuthorisations();

            RoleToAccessProfileDefinition roleToAccessProfileDefinition = caseTypeDefinition
                .getRoleToAccessProfile(roleAssignment.getRoleName());

            if (roleToAccessProfileDefinition != null && !roleToAccessProfileDefinition.getDisabled()) {
                List<String> authorisations = roleToAccessProfileDefinition.getAuthorisationList();

                if (roleAssignmentAuthorisations != null && roleAssignmentAuthorisations.size() > 0
                    && authorisations.size() > 0) {
                    Collection<String> filterAuthorisations = CollectionUtils
                        .intersection(roleAssignmentAuthorisations, authorisations);

                    if (filterAuthorisations.size() > 0) {
                        accessProfiles.addAll(createAccessProfiles(roleAssignment, roleToAccessProfileDefinition));
                    }
                } else if ((roleAssignmentAuthorisations == null || roleAssignmentAuthorisations.size() == 0)
                    && authorisations.size() == 0) {
                    accessProfiles.addAll(createAccessProfiles(roleAssignment, roleToAccessProfileDefinition));
                }
            }
        }
        return accessProfiles;
    }

    private List<AccessProfile> createAccessProfiles(RoleAssignment roleAssignment,
                                                     RoleToAccessProfileDefinition roleToAccessProfileDefinition) {
        String caseTypeAccessProfiles = roleToAccessProfileDefinition.getAccessProfiles();
        return Arrays.asList(caseTypeAccessProfiles.split(","))
            .stream()
            .map(accessProfileValue -> {
                AccessProfile accessProfile = new AccessProfile();

                accessProfile.setReadOnly(roleToAccessProfileDefinition.getReadOnly()
                    || roleAssignment.getReadOnly());
                accessProfile.setClassification(roleAssignment.getClassification());
                accessProfile.setAccessProfile(accessProfileValue);
                return accessProfile;
            }).collect(Collectors.toList());
    }
}
