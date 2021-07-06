package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.HashSet;
import java.util.Set;

public interface CaseDataAccessControl {
    Set<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId);

    Set<AccessProfile> generateAccessProfilesByCaseReference(String caseReference);

    default Set<AccessProfile> getCaseUserAccessProfilesByUserId() {
        return new HashSet<>();
    }

    default void grantAccess(String caseId, String idamUserId) {

    }

    CaseAccessMetadata generateAccessMetadata(String caseId);

    CaseAccessMetadata generateAccessMetadata(CaseTypeDefinition caseTypeDefinition);
}
