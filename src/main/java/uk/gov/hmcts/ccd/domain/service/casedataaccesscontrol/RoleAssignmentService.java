package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.ActorIdType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.Classification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoleAssignmentService implements AccessControl {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleAssignmentsMapper roleAssignmentsMapper;
    private final RoleAssignmentsFilteringService roleAssignmentsFilteringService;
    private final RoleAssignmentCategoryService roleAssignmentCategoryService;

    @Autowired
    public RoleAssignmentService(@Qualifier(CachedRoleAssignmentRepository.QUALIFIER)
                                         RoleAssignmentRepository roleAssignmentRepository,
                                 RoleAssignmentsMapper roleAssignmentsMapper,
                                 RoleAssignmentsFilteringService roleAssignmentsFilteringService,
                                 RoleAssignmentCategoryService roleAssignmentCategoryService) {
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.roleAssignmentsMapper = roleAssignmentsMapper;
        this.roleAssignmentsFilteringService = roleAssignmentsFilteringService;
        this.roleAssignmentCategoryService = roleAssignmentCategoryService;
    }

    public RoleAssignments createCaseRoleAssignments(final CaseDetails caseDetails,
                                                     final String userId,
                                                     final Set<String> roles,
                                                     final boolean replaceExisting) {

        RoleRequestResource roleRequest = RoleRequestResource.builder()
            .assignerId(userId)
            .process(RoleAssignmentRepository.DEFAULT_PROCESS)
            .reference(createRoleRequestReference(caseDetails, userId))
            .replaceExisting(replaceExisting)
            .build();

        List<RoleAssignmentResource> requestedRoles = roles.stream()
            .map(roleName -> RoleAssignmentResource.builder()
                .actorIdType(ActorIdType.IDAM.name())
                .actorId(userId)
                .roleType(RoleType.CASE.name())
                .roleName(roleName)
                .classification(Classification.RESTRICTED.name())
                .grantType(GrantType.SPECIFIC.name())
                .roleCategory(RoleCategory.PROFESSIONAL.name())
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
        // TODO: RDM-10924 - move roleCategory from here to the POST roleAssignments operation once it is implemented
        RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(userId);
        log.debug("user: {} has roleCategory: {}", userId, roleCategory);

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
                .filter(roleAssignments, caseTypeDefinition).getFilteredMatchingRoleAssignments();

        return getValidCaseIds(filteredRoleAssignments);
    }

    private List<String> getValidCaseIds(List<RoleAssignment> roleAssignmentsList) {
        return roleAssignmentsList.stream()
            .filter(this::isValidRoleAssignment)
            .map(roleAssignment -> roleAssignment.getAttributes().getCaseId())
            .filter(Objects::nonNull)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    private boolean isValidRoleAssignment(RoleAssignment roleAssignment) {
        final boolean isCaseRoleType = roleAssignment.getRoleType().equals(RoleType.CASE.name());
        return roleAssignment.isNotExpiredRoleAssignment() && isCaseRoleType;
    }

    public List<CaseAssignedUserRole> findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds) {
        final RoleAssignmentResponse roleAssignmentResponse =
            roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

        final RoleAssignments roleAssignments = roleAssignmentsMapper.toRoleAssignments(roleAssignmentResponse);
        var caseIdError = new RuntimeException(RoleAssignmentAttributes.ATTRIBUTE_NOT_DEFINED);
        return roleAssignments.getRoleAssignments().stream()
            .filter(roleAssignment -> isValidRoleAssignment(roleAssignment))
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
