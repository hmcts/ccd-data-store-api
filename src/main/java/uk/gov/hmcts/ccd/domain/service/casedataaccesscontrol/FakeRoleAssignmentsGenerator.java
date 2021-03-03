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
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.RESTRICTED;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType.SPECIFIC;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType.STANDARD;

@Component
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "true")
public class FakeRoleAssignmentsGenerator {

    protected static final String IDAM_PREFIX = "idam:";

    private final UserRepository userRepository;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseAccessService caseAccessService;

    @Autowired
    public FakeRoleAssignmentsGenerator(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                            CaseDefinitionRepository caseDefinitionRepository,
                                        CaseAccessService caseAccessService) {
        this.userRepository = userRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseAccessService = caseAccessService;
    }

    public List<RoleAssignment> addFakeRoleAssignments(List<RoleAssignment> roleAssignments) {
        List<String> idamUserRoles = new ArrayList<>(userRepository.getUserRoles());
        List<RoleAssignment> augmentedRoleAssignments = new ArrayList<>(roleAssignments);

        if (caseAccessService.canOnlyViewExplicitlyGrantedCases()) {
            if (atLeastOneCaseRoleExists(roleAssignments)) {
                augmentedRoleAssignments.addAll(createFakeRoleAssignmentsForGrantedOnlyAccess(idamUserRoles));
            }
        } else {
            augmentedRoleAssignments.addAll(createFakeRoleAssignments(idamUserRoles));
        }

        return augmentedRoleAssignments;
    }

    private List<RoleAssignment> createFakeRoleAssignmentsForGrantedOnlyAccess(List<String> idamRoles) {
        return idamRoles.stream()
            .map(role -> RoleAssignment.builder()
                .roleName(IDAM_PREFIX + role)
                .grantType(SPECIFIC.name())
                .classification(RESTRICTED.name())
                .build())
            .collect(Collectors.toList());
    }

    private List<RoleAssignment> createFakeRoleAssignments(List<String> idamRoles) {
        // TODO: potential performance issue here, to review
        List<UserRole> classifications = caseDefinitionRepository
            .getClassificationsForUserRoleList(idamRoles);

        Map<String, String> roleToClassification = classifications.stream()
            .collect(Collectors.toMap(UserRole::getRole, UserRole::getSecurityClassification));

        return idamRoles.stream()
            .map(role -> RoleAssignment.builder()
                .roleName(IDAM_PREFIX + role)
                .grantType(STANDARD.name())
                .classification(roleToClassification.get(role))
                .build())
            .collect(Collectors.toList());
    }

    private boolean atLeastOneCaseRoleExists(List<RoleAssignment> roleAssignments) {
        return roleAssignments.stream()
            .anyMatch(RoleAssignment::isCaseRoleAssignment);
    }
}
