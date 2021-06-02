package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;

public interface AccessProfileService {

    List<AccessProfile> generateAccessProfiles(RoleAssignmentFilteringResult filteringResults,
                                               List<RoleToAccessProfileDefinition> roleToAccessProfilesMappings);
}
