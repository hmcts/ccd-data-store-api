package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

@Component
public class PseudoRoleToAccessProfileGenerator {

    protected static final String IDAM_PREFIX = "idam:";
    private static final String CASE_ROLE_ID_REGEX = "^(\\[[A-Za-z]+\\])$";

    public List<RoleToAccessProfileDefinition> generate(CaseTypeDefinition caseTypeDefinition) {

        Set<String> caseRoles = extractCaseRoles(caseTypeDefinition);
        Set<String> idamRoles = extractIdamRoles(caseTypeDefinition);

        List<RoleToAccessProfileDefinition> roleToAccessProfiles = new ArrayList<>();
        roleToAccessProfiles.addAll(createRoleToAccessProfiles(caseTypeDefinition.getId(), caseRoles, false));
        roleToAccessProfiles.addAll(createRoleToAccessProfiles(caseTypeDefinition.getId(), idamRoles, true));

        return roleToAccessProfiles;
    }

    private Set<String> extractIdamRoles(CaseTypeDefinition caseTypeDefinition) {
        Set<String> idamRoles = new HashSet<>();

        idamRoles.addAll(getIdamRolesFromAcls(caseTypeDefinition.getAccessControlLists()));
        idamRoles.addAll(getIdamRolesFromAcls(eventAcls(caseTypeDefinition)));
        idamRoles.addAll(getIdamRolesFromAcls(stateAcls(caseTypeDefinition)));
        idamRoles.addAll(getIdamRolesFromAcls(caseFieldAcls(caseTypeDefinition)));
        idamRoles.addAll(getIdamRolesFromAcls(caseFieldComplexAcls(caseTypeDefinition)));

        return idamRoles;
    }

    private Set<String> extractCaseRoles(CaseTypeDefinition caseTypeDefinition) {
        Set<String> caseRoles = new HashSet<>();

        caseRoles.add(CREATOR.getRole());
        caseRoles.addAll(getCaseRolesFromAcls(caseTypeDefinition.getAccessControlLists()));
        caseRoles.addAll(getCaseRolesFromAcls(eventAcls(caseTypeDefinition)));
        caseRoles.addAll(getCaseRolesFromAcls(stateAcls(caseTypeDefinition)));
        caseRoles.addAll(getCaseRolesFromAcls(caseFieldAcls(caseTypeDefinition)));
        caseRoles.addAll(getCaseRolesFromAcls(caseFieldComplexAcls(caseTypeDefinition)));

        return caseRoles;
    }

    private List<String> getCaseRolesFromAcls(List<AccessControlList> accessControlLists) {
        return accessControlLists.stream()
            .map(AccessControlList::getAccessProfile)
            .filter(accessProfile -> accessProfile.matches(CASE_ROLE_ID_REGEX))
            .collect(Collectors.toList());
    }

    private List<String> getIdamRolesFromAcls(List<AccessControlList> accessControlLists) {
        return accessControlLists.stream()
            .map(AccessControlList::getAccessProfile)
            .filter(accessProfile -> !accessProfile.matches(CASE_ROLE_ID_REGEX))
            .collect(Collectors.toList());
    }

    private List<AccessControlList> eventAcls(CaseTypeDefinition caseTypeDefinition) {
        return caseTypeDefinition.getEvents().stream()
            .flatMap(element -> element.getAccessControlLists().stream())
            .collect(Collectors.toList());
    }

    private List<AccessControlList> stateAcls(CaseTypeDefinition caseTypeDefinition) {
        return caseTypeDefinition.getStates().stream()
            .flatMap(element -> element.getAccessControlLists().stream())
            .collect(Collectors.toList());
    }

    private List<AccessControlList> caseFieldAcls(CaseTypeDefinition caseTypeDefinition) {
        return caseTypeDefinition.getCaseFieldDefinitions().stream()
            .flatMap(element -> element.getAccessControlLists().stream())
            .collect(Collectors.toList());
    }

    private List<AccessControlList> caseFieldComplexAcls(CaseTypeDefinition caseTypeDefinition) {
        return caseTypeDefinition.getCaseFieldDefinitions().stream()
            .flatMap(element -> element.getComplexACLs().stream())
            .collect(Collectors.toList());
    }

    private List<RoleToAccessProfileDefinition> createRoleToAccessProfiles(String ctId,
                                                                           Set<String> roles,
                                                                           boolean addIdamPrefix) {
        return roles.stream()
            .map(role -> createRoleToAccessProfile(ctId, role, addIdamPrefix))
            .collect(Collectors.toList());
    }

    private RoleToAccessProfileDefinition createRoleToAccessProfile(String ctId, String role, boolean addIdamPrefix) {
        RoleToAccessProfileDefinition accessProfile = new RoleToAccessProfileDefinition();
        accessProfile.setAccessProfiles(role);
        accessProfile.setRoleName(addIdamPrefix ? IDAM_PREFIX + role : role);
        accessProfile.setCaseTypeId(ctId);
        accessProfile.setReadOnly(false);
        accessProfile.setAuthorisations(null);
        accessProfile.setReadOnly(false);
        accessProfile.setDisabled(false);
        return accessProfile;
    }
}
