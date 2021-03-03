package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.mapper;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public interface RoleAssignmentMapper {

    List<AccessProfile> map(List<RoleAssignment> roleAssignments,
                            CaseTypeDefinition caseTypeDefinition);
}
