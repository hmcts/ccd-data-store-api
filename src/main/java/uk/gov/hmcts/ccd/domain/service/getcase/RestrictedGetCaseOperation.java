package uk.gov.hmcts.ccd.domain.service.getcase;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentsFilteringService;

import java.util.Optional;
import java.util.stream.Stream;

import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleName.isValidRoleName;

@Service
@Qualifier("restricted")
public class RestrictedGetCaseOperation implements GetCaseOperation {

    private final GetCaseOperation defaultGetCaseOperation;
    private final GetCaseOperation authorisedGetCaseOperation;
    private final RoleAssignmentService roleAssignmentService;
    private final SecurityUtils securityUtils;
    private final RoleAssignmentsFilteringService roleAssignmentsFilteringService;

    public RestrictedGetCaseOperation(@Qualifier("default") final GetCaseOperation defaultGetCaseOperation,
                                      @Qualifier("authorised") final GetCaseOperation authorisedGetCaseOperation,
                                      SecurityUtils securityUtils,
                                      RoleAssignmentService roleAssignmentService,
                                      RoleAssignmentsFilteringService roleAssignmentsFilteringService) {
        this.defaultGetCaseOperation = defaultGetCaseOperation;
        this.authorisedGetCaseOperation = authorisedGetCaseOperation;
        this.securityUtils = securityUtils;
        this.roleAssignmentService = roleAssignmentService;
        this.roleAssignmentsFilteringService = roleAssignmentsFilteringService;

    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {
        return Optional.empty();
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        if (defaultGetCaseOperation.execute(caseReference).isPresent()) {
            // Get User's Role Assignments
            String userId = securityUtils.getUserId();
            RoleAssignments roleAssignmentsList = roleAssignmentService.getRoleAssignments(userId);
            filterRoleNames(roleAssignmentsList)
                .forEach(roleAssignments -> {
                    // filter role Assignments
                    // classification matcher ??
                   /* List<RoleAssignment> filteredRoleAssignments = roleAssignmentsFilteringService
                        .filter(roleAssignments, caseTypeDefinition).getFilteredMatchingRoleAssignments();*/
                });

        }
        return authorisedGetCaseOperation.execute(caseReference);
    }

    private Stream<RoleAssignment> filterRoleNames(RoleAssignments roleAssignmentsList) {
        return roleAssignmentsList.getRoleAssignments().stream()
            .filter(roleAssignment -> roleAssignment.getGrantType().equals(GrantType.BASIC.name())
                && roleAssignment.getRoleName().equals(isValidRoleName(roleAssignment.getRoleName())));
    }
}

