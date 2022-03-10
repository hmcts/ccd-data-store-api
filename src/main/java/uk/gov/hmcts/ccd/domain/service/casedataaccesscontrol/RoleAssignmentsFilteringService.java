package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.MatcherType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public interface RoleAssignmentsFilteringService {

    FilteredRoleAssignments filter(RoleAssignments roleAssignments, CaseDetails caseDetails);

    FilteredRoleAssignments filter(RoleAssignments roleAssignments, CaseTypeDefinition caseTypeDefinition);

    FilteredRoleAssignments filter(RoleAssignments roleAssignments,
                                   CaseTypeDefinition caseTypeDefinition,
                                   List<MatcherType> excludeMatching);
}
