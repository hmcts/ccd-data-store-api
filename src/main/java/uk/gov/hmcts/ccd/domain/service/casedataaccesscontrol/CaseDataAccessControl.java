package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.HashSet;
import java.util.Set;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;

public interface CaseDataAccessControl {

    Set<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId);

    Set<AccessProfile> generateAccessProfilesByCaseReference(String caseReference);

    default Set<AccessProfile> getCaseUserAccessProfilesByUserId() {
        return new HashSet<>();
    }

    default void grantAccess(String caseId, String idamUserId) {

    }
}
