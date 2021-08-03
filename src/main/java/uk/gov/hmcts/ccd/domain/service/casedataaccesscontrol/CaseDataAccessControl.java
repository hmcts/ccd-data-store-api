package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public interface CaseDataAccessControl {
    Set<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId);

    Set<AccessProfile> generateCreationAccessProfilesByCaseTypeId(String caseTypeId);

    Set<AccessProfile> generateAccessProfilesByCaseReference(String caseReference);

    default Set<AccessProfile> getCaseUserAccessProfilesByUserId() {
        return new HashSet<>();
    }

    default void grantAccess(String caseId, String idamUserId) {

    }

    CaseAccessMetadata generateAccessMetadataWithNoCaseId();

    CaseAccessMetadata generateAccessMetadata(String caseId);

    boolean anyAccessProfileEqualsTo(String caseTypeId, String accessProfile);

    boolean shouldRemoveCaseDefinition(Set<AccessProfile> accessProfiles,
                                       Predicate<AccessControlList> access,
                                       String caseTypeId);
}
