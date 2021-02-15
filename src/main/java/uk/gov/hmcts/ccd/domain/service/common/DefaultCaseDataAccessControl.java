package uk.gov.hmcts.ccd.domain.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "ccd.new-access-control-enabled", havingValue = "true")
public class DefaultCaseDataAccessControl implements CaseDataAccessControl, AccessControl {

    private final RoleAssignmentService roleAssignmentService;
    private final SecurityUtils securityUtils;
    private final CaseService caseService;

    @Autowired
    public DefaultCaseDataAccessControl(RoleAssignmentService roleAssignmentService,
                                        SecurityUtils securityUtils,
                                        CaseService caseService) {
        this.roleAssignmentService = roleAssignmentService;
        this.securityUtils = securityUtils;
        this.caseService = caseService;
    }

    // Returns Optional<CaseDetails>. If this is not enough think of wrapping it in a AccessControlResponse
    // that contains the additional information about the operation result
    @Override
    public Optional<CaseDetails> applyAccessControl(CaseDetails caseDetails) {
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(securityUtils.getUserId());
        CaseDetails cloned = caseService.clone(caseDetails);

        // 2.) Filter - Determine which of the role assignments are valid for the case by following the logic described
        // https://tools.hmcts.net/confluence/pages/viewpage.action?pageId=1399128967#HLD-CaseAccessControl-v1.1-2.7RoleAttributesandMatchingRules

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
