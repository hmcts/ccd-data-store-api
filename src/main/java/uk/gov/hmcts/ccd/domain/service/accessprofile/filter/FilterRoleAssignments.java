package uk.gov.hmcts.ccd.domain.service.accessprofile.filter;

import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public interface FilterRoleAssignments {

    RoleAssignments filter(RoleAssignments roleAssignments, CaseDetails caseDetails);

    RoleAssignments filter(RoleAssignments roleAssignments, CaseDataContent caseDataContent);

    RoleAssignments filter(RoleAssignments roleAssignments, CaseTypeDefinition caseTypeDefinition);
}
