package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.CachedRoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentAttributesResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRequestResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleRequestResource;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleAssignmentService implements AccessControl {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleAssignmentsMapper roleAssignmentsMapper;

    @Autowired
    public RoleAssignmentService(@Qualifier(CachedRoleAssignmentRepository.QUALIFIER)
                                         RoleAssignmentRepository roleAssignmentRepository,
                                 RoleAssignmentsMapper roleAssignmentsMapper) {
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.roleAssignmentsMapper = roleAssignmentsMapper;
    }

    public RoleAssignments createCaseRoleAssignments(final CaseDetails caseDetails,
                                                     final String userId,
                                                     final Set<String> roles,
                                                     final boolean replaceExisting) {

        RoleRequestResource roleRequest = RoleRequestResource.builder()
            .assignerId(userId)
            .process("CCD")
            .reference(createRoleRequestReference(caseDetails, userId))
            .replaceExisting(replaceExisting)
            .build();

        List<RoleAssignmentResource> requestedRoles = roles.stream()
            .map(roleName -> RoleAssignmentResource.builder()
                .actorIdType(RoleAssignmentRepository.ACTOR_ID_TYPE_IDAM)
                .actorId(userId)
                .roleType(RoleAssignmentRepository.ROLE_TYPE_CASE)
                .roleName(roleName)
                .classification((RoleAssignmentRepository.CLASSIFICATION_RESTRICTED))
                .grantType(GrantType.SPECIFIC.name())
                .roleCategory(RoleAssignmentRepository.ROLE_CATEGORY_PROFESSIONAL)
                .readOnly(false)
                .beginTime(Instant.now())
                .attributes(RoleAssignmentAttributesResource.builder()
                    .jurisdiction(Optional.of(caseDetails.getJurisdiction()))
                    .caseType(Optional.of(caseDetails.getCaseTypeId()))
                    .caseId(Optional.of(caseDetails.getReferenceAsString()))
                    .build())
                .build())
            .collect(Collectors.toList());

        RoleAssignmentRequestResource assignmentRequest = RoleAssignmentRequestResource.builder()
            .request(roleRequest)
            .requestedRoles(requestedRoles)
            .build();

        var roleAssignmentRequestResponse = roleAssignmentRepository.createRoleAssignment(assignmentRequest);
        return roleAssignmentsMapper.toRoleAssignments(roleAssignmentRequestResponse);
    }

    public RoleAssignments getRoleAssignments(String userId) {
        RoleAssignmentResponse roleAssignmentResponse = roleAssignmentRepository.getRoleAssignments(userId);
        return roleAssignmentsMapper.toRoleAssignments(roleAssignmentResponse);
    }

    public List<String> getCaseReferencesForAGivenUser(String userId) {
        final RoleAssignments roleAssignments = this.getRoleAssignments(userId);

        return roleAssignments.getRoleAssignments().stream()
            .filter(roleAssignment -> isAValidRoleAssignments(roleAssignment))
            .map(roleAssignment -> roleAssignment.getAttributes().getCaseId())
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    private boolean isAValidRoleAssignments(RoleAssignment roleAssignment) {
        final boolean isCaseRoleType = roleAssignment.getRoleType().equals(RoleType.CASE.name());
        return roleAssignment.isAnExpiredRoleAssignment() && isCaseRoleType;
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

    private String createRoleRequestReference(final CaseDetails caseDetails, final String userId) {
        return caseDetails.getReference() + "-" + userId;
    }

}
