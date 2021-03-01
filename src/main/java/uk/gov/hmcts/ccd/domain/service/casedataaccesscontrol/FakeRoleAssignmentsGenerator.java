package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Stream.concat;

@Component
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "true")
public class FakeRoleAssignmentsGenerator {

    private static final Pattern RESTRICT_GRANTED_ROLES_PATTERN =
        Pattern.compile(".+-solicitor$|.+-panelmember$|^citizen(-.*)?$|^letter-holder$|^caseworker-.+-localAuthority$");
    public static final String IDAM_PREFIX = "idam:";
    public static final String GRANT_TYPE_SPECIFIC = "SPECIFIC";
    public static final String GRANT_TYPE_STANDARD = "STANDARD";
    public static final String CLASSIFICATION_RESTRICTED = "RESTRICTED";
    public static final String ROLE_TYPE_CASE = "CASE";

    private final UserRepository userRepository;
    private final CaseDefinitionRepository caseDefinitionRepository;

    @Autowired
    public FakeRoleAssignmentsGenerator(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                            CaseDefinitionRepository caseDefinitionRepository) {
        this.userRepository = userRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    public List<RoleAssignment> addFakeRoleAssignments(List<RoleAssignment> roleAssignments) {
        List<String> idamUserRoles = new ArrayList<>(userRepository.getUserRoles());
        List<String> grantedRoles = filterGrantedRoles(idamUserRoles);
        List<RoleAssignment> augmentedRoleAssignments;

        if (!grantedRoles.isEmpty()) {
            if (atLeastOneCaseRoleExists(roleAssignments)) {
                augmentedRoleAssignments = concat(roleAssignments.stream(),
                                                  createFakeRoleAssignmentsGrantSpecific(grantedRoles).stream())
                    .collect(Collectors.toList());
            } else {
                augmentedRoleAssignments = new ArrayList<>(roleAssignments);
            }
        } else {
            augmentedRoleAssignments = concat(roleAssignments.stream(),
                                              createFakeRoleAssignmentsGrantStandard(idamUserRoles).stream())
                .collect(Collectors.toList());
        }

        return augmentedRoleAssignments;
    }

    private List<RoleAssignment> createFakeRoleAssignmentsGrantSpecific(List<String> grantedRoles) {
        return grantedRoles.stream()
            .map(role -> RoleAssignment.builder()
                .roleName(IDAM_PREFIX + role)
                .grantType(GRANT_TYPE_SPECIFIC)
                .classification(CLASSIFICATION_RESTRICTED)
                .build())
            .collect(Collectors.toList());
    }

    private List<RoleAssignment> createFakeRoleAssignmentsGrantStandard(List<String> grantedRoles) {
        List<UserRole> classifications = caseDefinitionRepository
            .getClassificationsForUserRoleList(grantedRoles);

        Map<String, String> roleToClassification = classifications.stream()
            .collect(Collectors.toMap(UserRole::getRole, UserRole::getSecurityClassification));

        return grantedRoles.stream()
            .map(role -> RoleAssignment.builder()
                .roleName(IDAM_PREFIX + role)
                .grantType(GRANT_TYPE_STANDARD)
                .classification(roleToClassification.get(role))
                .build())
            .collect(Collectors.toList());
    }

    private boolean atLeastOneCaseRoleExists(List<RoleAssignment> roleAssignments) {
        return roleAssignments.stream()
            .anyMatch(RoleAssignment::isCaseRoleAssignment);
    }

    public List<String> filterGrantedRoles(List<String> userRoles) {
        return userRoles
            .stream()
            .filter(role -> RESTRICT_GRANTED_ROLES_PATTERN.matcher(role).matches())
            .collect(Collectors.toList());
    }
}
