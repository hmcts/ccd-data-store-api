package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;

public interface CaseDataAccessControl {

    Set<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId);

    Set<AccessProfile> generateAccessProfilesByCaseReference(String caseReference);

    default Set<String> extractAccessProfileNames(Collection<AccessProfile> accessProfiles) {
        return accessProfiles.stream()
            .map(accessProfile -> accessProfile.getAccessProfile())
            .collect(Collectors.toSet());
    }

    void grantAccess(String caseId, String idamUserId);
}
