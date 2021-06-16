package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.CachedRoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleAssignmentService implements AccessControl {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleAssignmentsMapper roleAssignmentsMapper;
    private final RoleAssignmentsFilteringService roleAssignmentsFilteringService;

    @Autowired
    public RoleAssignmentService(@Qualifier(CachedRoleAssignmentRepository.QUALIFIER)
                                         RoleAssignmentRepository roleAssignmentRepository,
                                 RoleAssignmentsMapper roleAssignmentsMapper,
                                 RoleAssignmentsFilteringService roleAssignmentsFilteringService) {
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.roleAssignmentsMapper = roleAssignmentsMapper;
        this.roleAssignmentsFilteringService = roleAssignmentsFilteringService;
    }

    public RoleAssignments getRoleAssignments(String userId) {
        RoleAssignmentResponse roleAssignmentResponse = roleAssignmentRepository.getRoleAssignments(userId);
        return roleAssignmentsMapper.toRoleAssignments(roleAssignmentResponse);
    }

    public List<String> getCaseReferencesForAGivenUser(String userId) {
        final RoleAssignments roleAssignments = this.getRoleAssignments(userId);
        return getValidCaseIds(roleAssignments.getRoleAssignments());
    }

    public List<String> getCaseReferencesForAGivenUser(String userId, CaseTypeDefinition caseTypeDefinition) {

        final RoleAssignments roleAssignments = this.getRoleAssignments(userId);
        List<RoleAssignment> filteredRoleAssignments = roleAssignmentsFilteringService
                .filter(roleAssignments, caseTypeDefinition);

        return getValidCaseIds(filteredRoleAssignments);
    }

    private List<String> getValidCaseIds(List<RoleAssignment> roleAssignmentsList) {
        return roleAssignmentsList.stream()
            .filter(this::isAValidRoleAssignments)
            .map(roleAssignment -> roleAssignment.getAttributes().getCaseId())
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    private boolean isAValidRoleAssignments(RoleAssignment roleAssignment) {
        final boolean isCaseRoleType = roleAssignment.getRoleType().equals(RoleType.CASE.name());
        return roleAssignment.isNotExpiredRoleAssignment() && isCaseRoleType;
    }

    public List<CaseAssignedUserRole> findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds) {
        final RoleAssignmentResponse roleAssignmentResponse =
            roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

        final RoleAssignments roleAssignments = roleAssignmentsMapper.toRoleAssignments(roleAssignmentResponse);
        var caseIdError = new RuntimeException(RoleAssignmentAttributes.ATTRIBUTE_NOT_DEFINED);
        return roleAssignments.getRoleAssignments().stream()
            .filter(roleAssignment -> isAValidRoleAssignments(roleAssignment))
            .map(roleAssignment ->
                new CaseAssignedUserRole(
                    roleAssignment.getAttributes().getCaseId().orElseThrow(() -> caseIdError),
                    roleAssignment.getActorId(),
                    roleAssignment.getRoleName()
                )
            )
            .collect(Collectors.toList());
    }
}
