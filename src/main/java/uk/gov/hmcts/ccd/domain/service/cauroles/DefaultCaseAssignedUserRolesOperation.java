package uk.gov.hmcts.ccd.domain.service.cauroles;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.ccd.domain.service.caseaccess.CaseAccessOperation;

@Service
@Qualifier("default")
public class DefaultCaseAssignedUserRolesOperation implements CaseAssignedUserRolesOperation {

    private final CaseAccessOperation caseAccessOperation;

    @Autowired
    public DefaultCaseAssignedUserRolesOperation(CaseAccessOperation caseAccessOperation) {
        this.caseAccessOperation = caseAccessOperation;
    }

    public void addCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {
        this.caseAccessOperation.addCaseUserRoles(caseUserRoles);
    }

    @Override
    public void removeCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {
        this.caseAccessOperation.removeCaseUserRoles(caseUserRoles);
    }

    public List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseIds, List<String> userIds) {
        return this.caseAccessOperation.findCaseUserRoles(caseIds, userIds);
    }

}
