package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public interface RoleAssignmentsFilteringService {

    FilteredRoleAssignments filter(RoleAssignments roleAssignments, CaseDetails caseDetails);

    FilteredRoleAssignments filter(RoleAssignments roleAssignments, CaseTypeDefinition caseTypeDefinition);
}
