package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public interface RoleAssignmentsFilteringService {

    List<RoleAssignment> filter(RoleAssignments roleAssignments, CaseDetails caseDetails);

    List<RoleAssignment>  filter(RoleAssignments roleAssignments, CaseTypeDefinition caseTypeDefinition);
}
