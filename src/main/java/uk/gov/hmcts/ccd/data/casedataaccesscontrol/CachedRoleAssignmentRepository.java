package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newConcurrentMap;

@Service
@Qualifier(CachedRoleAssignmentRepository.QUALIFIER)
@RequestScope
public class CachedRoleAssignmentRepository implements RoleAssignmentRepository {

    private final RoleAssignmentRepository roleAssignmentRepository;

    public static final String QUALIFIER = "cached";

    private final Map<String, RoleAssignmentResponse> roleAssignments = newConcurrentMap();

    public CachedRoleAssignmentRepository(@Qualifier(DefaultRoleAssignmentRepository.QUALIFIER)
                                              RoleAssignmentRepository roleAssignmentRepository) {
        this.roleAssignmentRepository = roleAssignmentRepository;
    }

    @Override
    public RoleAssignmentRequestResponse createRoleAssignment(RoleAssignmentRequestResource assignmentRequest) {
        return roleAssignmentRepository.createRoleAssignment(assignmentRequest);
    }

    @Override
    public void deleteRoleAssignmentsByQuery(List<RoleAssignmentQuery> queryRequests) {
        roleAssignmentRepository.deleteRoleAssignmentsByQuery(queryRequests);
    }

    @Override
    public RoleAssignmentResponse getRoleAssignments(String userId) {
        return roleAssignments.computeIfAbsent(userId, e -> roleAssignmentRepository.getRoleAssignments(userId));
    }

    @Override
    public RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds) {
        return roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds,userIds);
    }

}
