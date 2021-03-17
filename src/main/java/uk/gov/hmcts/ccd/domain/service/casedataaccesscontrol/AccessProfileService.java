package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;

import java.util.List;

public interface AccessProfileService {

    List<AccessProfile> generateAccessProfiles(RoleAssignmentFilteringResult filteringResults,
                                               List<RoleToAccessProfileDefinition> roleToAccessProfilesMappings);
}
