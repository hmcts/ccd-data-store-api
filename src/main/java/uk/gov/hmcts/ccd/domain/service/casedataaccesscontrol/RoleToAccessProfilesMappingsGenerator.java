package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

@Component
public class RoleToAccessProfilesMappingsGenerator {

    protected static final String IDAM_PREFIX = "idam:";
    private static final String CASE_ROLE_ID_REGEX = "^(\\[[A-Za-z]+\\])$";

    private final CaseRoleRepository caseRoleRepository;

    public RoleToAccessProfilesMappingsGenerator(@Qualifier(CachedCaseRoleRepository.QUALIFIER)
                                                     CaseRoleRepository caseRoleRepository) {
        this.caseRoleRepository = caseRoleRepository;
    }

    public List<AccessProfile> generate(CaseTypeDefinition caseTypeDefinition) {

        Set<String> caseRoles = generateCaseRoles(caseTypeDefinition);
        Set<String> idamRoles = generateIdamRoles(caseTypeDefinition);

        List<AccessProfile> accessProfiles = new ArrayList<>();
        accessProfiles.addAll(createAccessProfiles(caseRoles, false));
        accessProfiles.addAll(createAccessProfiles(idamRoles, true));

        return accessProfiles;
    }

    private Set<String> generateIdamRoles(CaseTypeDefinition caseTypeDefinition) {
        Set<String> idamRoles = new HashSet<>();

        idamRoles.addAll(getIdamRolesFromAcls(caseTypeDefinition.getAccessControlLists()));
        idamRoles.addAll(getIdamRolesFromAcls(eventAcls(caseTypeDefinition)));
        idamRoles.addAll(getIdamRolesFromAcls(stateAcls(caseTypeDefinition)));
        idamRoles.addAll(getIdamRolesFromAcls(caseFieldAcls(caseTypeDefinition)));
        idamRoles.addAll(getIdamRolesFromAcls(caseFieldComplexAcls(caseTypeDefinition)));

        return idamRoles;
    }

    private Set<String> generateCaseRoles(CaseTypeDefinition caseTypeDefinition) {
        final Set<String> validCaseRoles = caseRoleRepository.getCaseRoles(caseTypeDefinition.getId());
        boolean containsCreator = validCaseRoles.contains(CREATOR.getRole());

        Set<String> caseRoles = new HashSet<>();
        if (!containsCreator) {
            caseRoles.add(CREATOR.getRole());
        }
        caseRoles.addAll(getCaseRolesFromAcls(caseTypeDefinition.getAccessControlLists()));
        caseRoles.addAll(getCaseRolesFromAcls(eventAcls(caseTypeDefinition)));
        caseRoles.addAll(getCaseRolesFromAcls(stateAcls(caseTypeDefinition)));
        caseRoles.addAll(getCaseRolesFromAcls(caseFieldAcls(caseTypeDefinition)));
        caseRoles.addAll(getCaseRolesFromAcls(caseFieldComplexAcls(caseTypeDefinition)));

        return caseRoles;
    }

    private List<String> getCaseRolesFromAcls(List<AccessControlList> accessControlLists) {
        return accessControlLists.stream()
            .map(AccessControlList::getRole)
            .filter(role -> role.matches(CASE_ROLE_ID_REGEX))
            .collect(Collectors.toList());
    }

    private List<String> getIdamRolesFromAcls(List<AccessControlList> accessControlLists) {
        return accessControlLists.stream()
            .map(AccessControlList::getRole)
            .filter(role -> !role.matches(CASE_ROLE_ID_REGEX))
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

    private List<AccessProfile> createAccessProfiles(Set<String> roles, boolean addIdamPrefix) {
        return roles.stream()
            .map(role -> createAccessProfile(role, addIdamPrefix))
            .collect(Collectors.toList());
    }

    private AccessProfile createAccessProfile(String role, boolean addIdamPrefix) {
        AccessProfile accessProfile = new AccessProfile();
        accessProfile.setAccessProfiles(Collections.singletonList(role));
        accessProfile.setRoleName(addIdamPrefix ? IDAM_PREFIX + role : role);
        accessProfile.setReadOnly(false);
        return accessProfile;
    }
}
