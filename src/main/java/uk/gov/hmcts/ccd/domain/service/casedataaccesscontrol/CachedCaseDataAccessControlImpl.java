package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

import static com.google.common.collect.Maps.newConcurrentMap;

@Service
@RequestScope
public class CachedCaseDataAccessControlImpl implements CaseDataAccessControl, AccessControl {

    private final NoCacheCaseDataAccessControl noCacheCaseDataAccessControl;

    private final Map<String, Set<AccessProfile>> caseTypeAccessProfiles = newConcurrentMap();

    private final Map<String, Set<AccessProfile>> caseTypeOrganisationalAccessProfiles = newConcurrentMap();

    private final Map<String, Set<AccessProfile>> caseReferenceAccessProfiles = newConcurrentMap();

    private final Map<String, List<RoleAssignment>> caseTypeRoleAssignments = newConcurrentMap();


    @Autowired
    public CachedCaseDataAccessControlImpl(NoCacheCaseDataAccessControl noCacheCaseDataAccessControl) {
        this.noCacheCaseDataAccessControl = noCacheCaseDataAccessControl;
    }

    @Override
    public Set<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId) {
        return caseTypeAccessProfiles.computeIfAbsent(caseTypeId,
            e -> noCacheCaseDataAccessControl.generateAccessProfilesByCaseTypeId(caseTypeId));
    }

    @Override
    public Set<AccessProfile> generateOrganisationalAccessProfilesByCaseTypeId(String caseTypeId) {
        return caseTypeOrganisationalAccessProfiles.computeIfAbsent(caseTypeId,
            e -> noCacheCaseDataAccessControl.generateOrganisationalAccessProfilesByCaseTypeId(caseTypeId));
    }

    @Override
    public Set<AccessProfile> generateAccessProfilesByCaseReference(String caseReference) {
        return caseReferenceAccessProfiles.computeIfAbsent(caseReference,
            e -> noCacheCaseDataAccessControl.generateAccessProfilesByCaseReference(caseReference));
    }

    @Override
    public Set<AccessProfile> generateAccessProfilesByCaseDetails(CaseDetails caseDetails) {
        return caseReferenceAccessProfiles.computeIfAbsent(caseDetails.getReferenceAsString(),
            e -> noCacheCaseDataAccessControl.generateAccessProfilesByCaseDetails(caseDetails));
    }

    public Set<AccessProfile> getCaseUserAccessProfilesByUserId() {
        return noCacheCaseDataAccessControl.getCaseUserAccessProfilesByUserId();
    }

    @Override
    public void grantAccess(CaseDetails caseDetails, String idamUserId) {
        noCacheCaseDataAccessControl.grantAccess(caseDetails, idamUserId);
    }

    @Override
    public CaseAccessMetadata generateAccessMetadata(String caseId) {
        return noCacheCaseDataAccessControl.generateAccessMetadata(caseId);
    }

    @Override
    public CaseAccessMetadata generateAccessMetadataWithNoCaseId() {
        return noCacheCaseDataAccessControl.generateAccessMetadataWithNoCaseId();
    }

    @Override
    public boolean anyAccessProfileEqualsTo(String caseTypeId, String accessProfile) {
        return noCacheCaseDataAccessControl.anyAccessProfileEqualsTo(caseTypeId, accessProfile);
    }

    @Override
    public boolean shouldRemoveCaseDefinition(Set<AccessProfile> accessProfiles,
                                              Predicate<AccessControlList> access,
                                              String caseTypeId) {
        return noCacheCaseDataAccessControl.shouldRemoveCaseDefinition(accessProfiles,
            access,
            caseTypeId);
    }

    @Override
    public Set<AccessProfile> filteredAccessProfiles(List<RoleAssignment> filteredRoleAssignments,
                                               CaseTypeDefinition caseTypeDefinition,
                                               boolean isCreationProfile) {
        return Sets.newHashSet(noCacheCaseDataAccessControl.filteredAccessProfiles(filteredRoleAssignments,
            caseTypeDefinition,
            isCreationProfile));
    }

    @Override
    public List<RoleAssignment> generateRoleAssignments(CaseTypeDefinition caseTypeDefinition) {
        return caseTypeRoleAssignments.computeIfAbsent(caseTypeDefinition.getId(),
            e -> noCacheCaseDataAccessControl.generateRoleAssignments(caseTypeDefinition));
    }
}
