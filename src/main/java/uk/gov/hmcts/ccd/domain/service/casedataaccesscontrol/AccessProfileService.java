package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;

public interface AccessProfileService {

    List<AccessProfile> generateAccessProfiles(List<RoleAssignment> filteredRoleAssignments,
                                               List<RoleToAccessProfileDefinition> roleToAccessProfilesMappings);
}
