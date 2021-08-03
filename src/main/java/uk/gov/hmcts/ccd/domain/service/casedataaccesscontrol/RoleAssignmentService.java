package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.CachedRoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.MatcherType;
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

    public RoleAssignments getRoleAssignments(String userId) {
        // TODO: RDM-10924 - move roleCategory from here to the POST roleAssignments operation once it is implemented
        RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(userId);
        log.debug("user: {} has roleCategory: {}", userId, roleCategory);
        RoleAssignmentResponse roleAssignmentResponse = roleAssignmentRepository.getRoleAssignments(userId);
        return roleAssignmentsMapper.toRoleAssignments(roleAssignmentResponse);
    }

    public List<RoleAssignment> getRoleAssignments(String userId, CaseTypeDefinition caseTypeDefinition) {
        final RoleAssignments roleAssignments = this.getRoleAssignments(userId);
        return roleAssignmentsFilteringService
            .filter(roleAssignments, caseTypeDefinition, Lists.newArrayList(MatcherType.GRANTTYPE))
            .getFilteredMatchingRoleAssignments();
    }

    public RoleAssignments getRoleAssignmentsForCreate(String userId) {
        final var roleAssignments = getRoleAssignments(userId);
        return getOrganisationRA(roleAssignments.getRoleAssignments());
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
}
