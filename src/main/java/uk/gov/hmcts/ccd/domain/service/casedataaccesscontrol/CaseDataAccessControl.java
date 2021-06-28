package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;

import java.util.HashSet;
import java.util.Set;

public interface CaseDataAccessControl {
    boolean CHECK_REGION_LOCATION_TRUE = true;

    boolean CHECK_REGION_LOCATION_FALSE = false;

    Set<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId);

    Set<AccessProfile> generateAccessProfilesByCaseReference(String caseReference);

    default Set<AccessProfile> getCaseUserAccessProfilesByUserId() {
        return new HashSet<>();
    }

    default void grantAccess(String caseId, String idamUserId) {

    }

    CaseAccessMetadata generateAccessMetadata(String caseId, boolean isCheckingRegionLocationFilteringChecks);
}
