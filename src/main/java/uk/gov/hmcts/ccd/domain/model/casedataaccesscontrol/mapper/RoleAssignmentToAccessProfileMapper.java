package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.mapper;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public interface RoleAssignmentToAccessProfileMapper {

    List<AccessProfile> toAccessProfiles(RoleAssignmentFilteringResult filteringResult,
                                         CaseTypeDefinition caseTypeDefinition);
}
