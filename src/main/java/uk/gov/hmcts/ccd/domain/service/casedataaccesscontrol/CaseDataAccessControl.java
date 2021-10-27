package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public interface CaseDataAccessControl {
    Set<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId);

    Set<AccessProfile> generateOrganisationalAccessProfilesByCaseTypeId(String caseTypeId);

    Set<AccessProfile> generateAccessProfilesByCaseReference(String caseReference);

    default Set<AccessProfile> getCaseUserAccessProfilesByUserId() {
        return new HashSet<>();
    }

    void grantAccess(CaseDetails caseDetails, String idamUserId);

    CaseAccessMetadata generateAccessMetadataWithNoCaseId();

    CaseAccessMetadata generateAccessMetadata(String caseId);

    boolean anyAccessProfileEqualsTo(String caseTypeId, String accessProfile);

    boolean shouldRemoveCaseDefinition(Set<AccessProfile> accessProfiles,
                                       Predicate<AccessControlList> access,
                                       String caseTypeId);
}
