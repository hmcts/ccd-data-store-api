package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleAssignmentService implements AccessControl {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleAssignmentsMapper roleAssignmentsMapper;

    @Autowired
    public RoleAssignmentService(RoleAssignmentRepository roleAssignmentRepository,
                                 RoleAssignmentsMapper roleAssignmentsMapper) {
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.roleAssignmentsMapper = roleAssignmentsMapper;
    }

    public RoleAssignments getRoleAssignments(String userId) {
        RoleAssignmentResponse roleAssignmentResponse = roleAssignmentRepository.getRoleAssignments(userId);
        return roleAssignmentsMapper.toRoleAssignments(roleAssignmentResponse);
    }

        public List<String> getCaseIdsForAGivenUser(String userId) {
        final RoleAssignments roleAssignments = this.getRoleAssignments(userId);

        final List<String> result = roleAssignments.getRoleAssignments().stream()
            .filter(roleAssignment -> isAValidRoleAssignments(roleAssignment))
            .map(
                roleAssignment -> roleAssignment.getAttributes().getCaseId()
            ).flatMap(Optional::stream)
            .collect(Collectors.toList());
        return result;
    }

    private boolean isAValidRoleAssignments(RoleAssignment roleAssignment ){
        final boolean isACaseType = roleAssignment.getRoleType().equals(RoleType.CASE.name());
        return roleAssignment.isAnExpiredRoleAssignment() && isACaseType;
    }
}
