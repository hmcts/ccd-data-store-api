package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.RESTRICTED;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.EXCLUDED;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.SPECIFIC;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.STANDARD;
import static uk.gov.hmcts.ccd.domain.service.AccessControl.IDAM_PREFIX;

@Component
public class PseudoRoleAssignmentsGenerator {

    private final UserRepository userRepository;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseAccessService caseAccessService;

    @Autowired
    public PseudoRoleAssignmentsGenerator(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                          @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                            CaseDefinitionRepository caseDefinitionRepository,
                                          CaseAccessService caseAccessService) {
        this.userRepository = userRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseAccessService = caseAccessService;
    }

    public List<RoleAssignment> createPseudoRoleAssignments(List<RoleAssignment> filteredRoleAssignments,
                                                            boolean isCreationProfile) {

        List<String> idamUserRoles = new ArrayList<>(userRepository.getUserRoles());
        List<RoleAssignment> pseudoRoleAssignments = new ArrayList<>();

        if (!isCreationProfile && caseAccessService.userCanOnlyAccessExplicitlyGrantedCases()) {
            if (atLeastOneNonExcludedCaseRoleExists(filteredRoleAssignments)) {
                pseudoRoleAssignments.addAll(createPseudoRoleAssignmentsForGrantedOnlyAccess(idamUserRoles));
            }
        } else {
            pseudoRoleAssignments.addAll(createPseudoRoleAssignmentsByIdamRoles(idamUserRoles));
        }

        return pseudoRoleAssignments;
    }

    private List<RoleAssignment> createPseudoRoleAssignmentsByIdamRoles(List<String> idamRoles) {
        // TODO: potential performance issue here, to review
        List<UserRole> classifications = caseDefinitionRepository
            .getClassificationsForUserRoleList(idamRoles);

        Map<String, String> roleToClassification = classifications.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(UserRole::getRole, UserRole::getSecurityClassification));

        return idamRoles.stream()
            .map(role -> RoleAssignment.builder()
                .roleName(IDAM_PREFIX + role)
                .grantType(STANDARD.name())
                .classification(roleToClassification.get(role))
                .build())
            .collect(Collectors.toList());
    }

    private List<RoleAssignment> createPseudoRoleAssignmentsForGrantedOnlyAccess(List<String> idamRoles) {
        return idamRoles.stream()
            .map(role -> RoleAssignment.builder()
                .roleName(IDAM_PREFIX + role)
                .grantType(SPECIFIC.name())
                .classification(RESTRICTED.name())
                .build())
            .collect(Collectors.toList());
    }

    private boolean atLeastOneNonExcludedCaseRoleExists(List<RoleAssignment> roleAssignments) {
        return roleAssignments
            .stream()
            .filter(roleAssignment -> !roleAssignment.isGrantType(EXCLUDED))
            .anyMatch(RoleAssignment::isCaseRoleAssignment);
    }
}
