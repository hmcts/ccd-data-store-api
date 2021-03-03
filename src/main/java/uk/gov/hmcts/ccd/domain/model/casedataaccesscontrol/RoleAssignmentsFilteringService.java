package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public interface RoleAssignmentsFilteringService {

    RoleAssignmentFilteringResult filter(RoleAssignments roleAssignments, CaseDetails caseDetails);

    RoleAssignmentFilteringResult filter(RoleAssignments roleAssignments, CaseDataContent caseDataContent);

    RoleAssignmentFilteringResult filter(RoleAssignments roleAssignments, CaseTypeDefinition caseTypeDefinition);
}
