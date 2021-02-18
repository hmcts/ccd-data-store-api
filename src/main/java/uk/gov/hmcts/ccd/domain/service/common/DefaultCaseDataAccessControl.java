package uk.gov.hmcts.ccd.domain.service.common;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.FilterRoleAssignments;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;

@Component
@ConditionalOnProperty(name = "ccd.new-access-control-enabled", havingValue = "true")
public class DefaultCaseDataAccessControl implements CaseDataAccessControl, AccessControl {

    private final RoleAssignmentService roleAssignmentService;
    private final SecurityUtils securityUtils;
    private final CaseService caseService;
    private FilterRoleAssignments filterRoleAssignments;

    @Autowired
    public DefaultCaseDataAccessControl(RoleAssignmentService roleAssignmentService,
                                        SecurityUtils securityUtils,
                                        CaseService caseService,
                                        FilterRoleAssignments filterRoleAssignments) {
        this.roleAssignmentService = roleAssignmentService;
        this.securityUtils = securityUtils;
        this.caseService = caseService;
        this.filterRoleAssignments = filterRoleAssignments;
    }

    // Returns Optional<CaseDetails>. If this is not enough think of wrapping it in a AccessControlResponse
    // that contains the additional information about the operation result
    @Override
    public Optional<CaseDetails> applyAccessControl(CaseDetails caseDetails) {
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(securityUtils.getUserId());
        CaseDetails cloned = caseService.clone(caseDetails);

        roleAssignments = filterRoleAssignments.filter(roleAssignments, caseDetails);

        // 3.) Augment - Add to the list of filtered roles entries corresponding to the users Idam roles prefixed
        // by 'idam:' (subject to a column that states whether these roles apply to case specific roles)

        // 4.) determine AccessProfiles from the new RoleToAccessProfiles Tab
        // https://tools.hmcts.net/confluence/pages/viewpage.action?pageId=1460559903#AccessControlScopeofDelivery-NewRoleToAccessProfilesTab
        // as a result we identify the AccessProfiles that the user has on the case

        return Optional.of(cloned);
    }

    @Override
    public void grantAccess(String caseId, String idamUserId) {

    }
}
