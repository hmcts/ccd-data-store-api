package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.CachedRoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.CITIZEN;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.ENFORCEMENT;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.JUDICIAL;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.LEGAL_OPERATIONS;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.PROFESSIONAL;

@Service
public class RoleAssignmentCategoryService {

    private static final Pattern PROFESSIONAL_ROLE =
        Pattern.compile(".+-solicitor$|^caseworker-.+-localAuthority$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CITIZEN_ROLE =
        Pattern.compile("^citizen(-.*)?$|^letter-holder$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JUDICIAL_ROLE = Pattern.compile(".+-panelmember$",
        Pattern.CASE_INSENSITIVE);
    private static final List<String> ENFORCEMENT_ROLES = List.of("bailiff-manager", "bailiff");

    private final IdamRepository idamRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleAssignmentsMapper roleAssignmentsMapper;

    public RoleAssignmentCategoryService(IdamRepository idamRepository,
                                           @Qualifier(CachedRoleAssignmentRepository.QUALIFIER)
                                           RoleAssignmentRepository roleAssignmentRepository,
                                           RoleAssignmentsMapper roleAssignmentsMapper) {
        this.idamRepository = idamRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.roleAssignmentsMapper = roleAssignmentsMapper;
    }

    public RoleCategory getRoleCategory(String userId) {
        List<String> idamUserRoles = idamRepository.getUserRoles(userId);


        if (hasProfessionalRole(idamUserRoles)) {
            return PROFESSIONAL;
        } else if (hasCitizenRole(idamUserRoles)) {
            return CITIZEN;
        } else if (hasJudicialRole(idamUserRoles)) {
            return JUDICIAL;
        } else if (hasEnforcementRole(userId)) {
            return ENFORCEMENT;
        } else {
            return LEGAL_OPERATIONS;
        }
    }

    private boolean hasProfessionalRole(List<String> roles) {
        return roles.stream().anyMatch(role -> PROFESSIONAL_ROLE.matcher(role).matches());
    }

    private boolean hasCitizenRole(List<String> roles) {
        return roles.stream().anyMatch(role -> CITIZEN_ROLE.matcher(role).matches());
    }

    private boolean hasJudicialRole(List<String> roles) {
        return roles.stream().anyMatch(role -> JUDICIAL_ROLE.matcher(role).matches());
    }

    private boolean hasEnforcementRole(String userId) {
        RoleAssignments roleAssignments;
        try {
            roleAssignments = roleAssignmentsMapper.toRoleAssignments(roleAssignmentRepository
                .getRoleAssignments(userId));
        } catch (ResourceNotFoundException ex) {
            return false;
        }
        List<RoleAssignment> assignments = roleAssignments == null || roleAssignments.getRoleAssignments() == null
            ? Collections.emptyList()
            : roleAssignments.getRoleAssignments();

        return assignments.stream()
            .filter(roleAssignment -> roleAssignment.isGrantType(GrantType.STANDARD))
            .map(RoleAssignment::getRoleName)
            .anyMatch(ENFORCEMENT_ROLES::contains);
    }
}
