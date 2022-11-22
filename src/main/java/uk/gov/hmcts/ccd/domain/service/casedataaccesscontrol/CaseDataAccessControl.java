package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

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

public interface CaseDataAccessControl {
    Set<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId);

    Set<AccessProfile> generateOrganisationalAccessProfilesByCaseTypeId(String caseTypeId);

    Set<AccessProfile> generateAccessProfilesByCaseReference(String caseReference);

    Set<AccessProfile> generateAccessProfilesByCaseDetails(CaseDetails caseDetails);

    Set<AccessProfile> generateAccessProfilesForRestrictedCase(CaseDetails caseDetails);

    Set<AccessProfile> getCaseUserAccessProfilesByUserId();

    void grantAccess(CaseDetails caseDetails, String idamUserId);

    CaseAccessMetadata generateAccessMetadataWithNoCaseId();

    CaseAccessMetadata generateAccessMetadata(String caseId);

    boolean anyAccessProfileEqualsTo(String caseTypeId, String accessProfile);

    boolean shouldRemoveCaseDefinition(Set<AccessProfile> accessProfiles,
                                       Predicate<AccessControlList> access,
                                       String caseTypeId);

    Set<AccessProfile> filteredAccessProfiles(List<RoleAssignment> filteredRoleAssignments,
                                               CaseTypeDefinition caseTypeDefinition,
                                               boolean isCreationProfile);

    List<RoleAssignment> generateRoleAssignments(CaseTypeDefinition caseTypeDefinition);

    Set<SecurityClassification> getUserClassifications(CaseTypeDefinition caseTypeDefinition, boolean isCreateProfile);

    Set<SecurityClassification> getUserClassifications(CaseDetails caseDetails);

    SecurityClassification getHighestUserClassification(CaseTypeDefinition caseTypeDefinition,
                                                        boolean isCreateProfile);
}
