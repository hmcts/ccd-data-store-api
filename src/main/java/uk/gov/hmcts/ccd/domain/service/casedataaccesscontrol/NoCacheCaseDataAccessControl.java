package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Lists;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public interface NoCacheCaseDataAccessControl {

    Set<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId);

    Set<AccessProfile> generateOrganisationalAccessProfilesByCaseTypeId(String caseTypeId);

    Set<AccessProfile> generateAccessProfilesByCaseReference(String caseReference);

    Set<AccessProfile> generateAccessProfilesByCaseDetails(CaseDetails caseDetails);

    Set<AccessProfile> generateAccessProfilesForRestrictedCase(CaseDetails caseDetails);

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

    default List<AccessProfile> filteredAccessProfiles(List<RoleAssignment> filteredRoleAssignments,
                                                       CaseTypeDefinition caseTypeDefinition,
                                                       boolean isCreationProfile) {
        return Lists.newArrayList();
    }

    default List<RoleAssignment> generateRoleAssignments(CaseTypeDefinition caseTypeDefinition) {
        return Lists.newArrayList();
    }

    Set<SecurityClassification> getUserClassifications(CaseTypeDefinition caseTypeDefinition, boolean isCreateProfile);

    Set<SecurityClassification> getUserClassifications(CaseDetails caseDetails);
}
