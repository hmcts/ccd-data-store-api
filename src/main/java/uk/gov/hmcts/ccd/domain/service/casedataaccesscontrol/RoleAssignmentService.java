package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.CachedRoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentAttributesResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentQuery;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRequestResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleRequestResource;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentsDeleteRequest;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.ActorIdType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.Classification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.MatcherType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

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

        var roleCategory = roleAssignmentCategoryService.getRoleCategory(userId);
        log.debug("user: {} has roleCategory: {}", userId, roleCategory);

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
                .roleCategory(roleCategory.name())
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
            .roleRequest(roleRequest)
            .requestedRoles(requestedRoles)
            .build();

        var roleAssignmentRequestResponse = roleAssignmentRepository.createRoleAssignment(assignmentRequest);
        return roleAssignmentsMapper.toRoleAssignments(roleAssignmentRequestResponse);
    }

    public void deleteRoleAssignments(List<RoleAssignmentsDeleteRequest> deleteRequests) {
        if (deleteRequests != null && !deleteRequests.isEmpty()) {
            List<RoleAssignmentQuery> queryRequests = deleteRequests.stream()
                .map(request -> new RoleAssignmentQuery(
                    request.getCaseId(),
                    request.getUserId(),
                    request.getRoleNames())
                )
                .collect(Collectors.toList());

            roleAssignmentRepository.deleteRoleAssignmentsByQuery(queryRequests);
        }
    }

    public RoleAssignments getRoleAssignments(String userId) {
        final var roleAssignmentResponse = roleAssignmentRepository.getRoleAssignments(userId);
        return roleAssignmentsMapper.toRoleAssignments(roleAssignmentResponse);
    }

    public List<RoleAssignment> getRoleAssignments(String userId, CaseTypeDefinition caseTypeDefinition) {
        final RoleAssignments roleAssignments = this.getRoleAssignments(userId);
        return roleAssignmentsFilteringService
            .filter(roleAssignments, caseTypeDefinition,
                Lists.newArrayList(
                    MatcherType.GRANTTYPE,
                    MatcherType.SECURITYCLASSIFICATION,
                    MatcherType.AUTHORISATION
                )
            ).getFilteredMatchingRoleAssignments();
    }

    public RoleAssignments getRoleAssignmentsForCreate(String userId) {
        final var roleAssignments = getRoleAssignments(userId);
        return getOrganisationRA(roleAssignments.getRoleAssignments());
    }

    public List<String> getCaseReferencesForAGivenUser(String userId) {
        final var roleAssignments = this.getRoleAssignments(userId);
        return getValidCaseIds(roleAssignments.getRoleAssignments());
    }

    public List<String> getCaseReferencesForAGivenUser(String userId, CaseTypeDefinition caseTypeDefinition) {

        final var roleAssignments = this.getRoleAssignments(userId);
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
            .distinct()
            .collect(Collectors.toList());
    }

    private RoleAssignments getOrganisationRA(List<RoleAssignment> roleAssignmentsList) {
        return RoleAssignments.builder().roleAssignments(roleAssignmentsList.stream()
            .filter(this::isValidOrganisation)
            .collect(Collectors.toList())).build();
    }

    private boolean isValidRoleAssignment(RoleAssignment roleAssignment) {
        final boolean isCaseRoleType = roleAssignment.getRoleType().equals(RoleType.CASE.name());
        return roleAssignment.isNotExpiredRoleAssignment() && isCaseRoleType;
    }

    private boolean isValidOrganisation(RoleAssignment roleAssignment) {
        final boolean isOrgRole = roleAssignment.getRoleType().equals(RoleType.ORGANISATION.name());
        return roleAssignment.isNotExpiredRoleAssignment() && isOrgRole;
    }

    public List<CaseAssignedUserRole> findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds) {
        final var roleAssignmentResponse =
            roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

        final var roleAssignments = roleAssignmentsMapper.toRoleAssignments(roleAssignmentResponse);
        var caseIdError = new RuntimeException(RoleAssignmentAttributes.ATTRIBUTE_NOT_DEFINED);
        return roleAssignments.getRoleAssignments().stream()
            .filter(this::isValidRoleAssignment)
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
