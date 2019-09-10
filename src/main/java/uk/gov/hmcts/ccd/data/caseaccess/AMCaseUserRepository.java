package uk.gov.hmcts.ccd.data.caseaccess;

import com.fasterxml.jackson.core.JsonPointer;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.reform.amlib.AccessManagementService;
import uk.gov.hmcts.reform.amlib.enums.AccessorType;
import uk.gov.hmcts.reform.amlib.enums.Permission;
import uk.gov.hmcts.reform.amlib.models.ExplicitAccessGrant;
import uk.gov.hmcts.reform.amlib.models.ExplicitAccessMetadata;
import uk.gov.hmcts.reform.amlib.models.ResourceDefinition;
import uk.gov.hmcts.reform.amlib.models.UserCaseRolesEnvelope;
import uk.gov.hmcts.reform.amlib.models.UserCasesEnvelope;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.amlib.enums.Permission.READ;

@Named
@Singleton
@Qualifier(AMCaseUserRepository.ACCESS_MANAGEMENT_QUALIFIER)
public class AMCaseUserRepository implements CaseUserRepository {

    public static final String ACCESS_MANAGEMENT_QUALIFIER = "accessManagement";
    private static final String CASE_CONSTANT = "case";

    private AccessManagementService accessManagementService;

    public AMCaseUserRepository(@Qualifier("amDataSource") DataSource dataSource) {
        accessManagementService = new AccessManagementService(dataSource);
    }

    @Override
    @Transactional
    public void grantAccess(String jurisdictionId, String caseTypeId, String caseReference, Long caseId, String userId, String caseRole) {
        ResourceDefinition resourceDefinition =
            //TODO: What should be the resourceType and resourceName.
            new ResourceDefinition(jurisdictionId, CASE_CONSTANT, caseReference);

        ExplicitAccessGrant explicitAccessGrant = ExplicitAccessGrant.builder()
            .accessorType(AccessorType.USER)
            .accessorIds(Collections.singleton(userId))
            .resourceId(caseId.toString())
            .attributePermissions(getAttributePermissions())
            .resourceDefinition(resourceDefinition)
            .relationship(caseRole)
            .build();

        accessManagementService.grantExplicitResourceAccess(explicitAccessGrant);
    }

    private Map<JsonPointer, Set<Permission>> getAttributePermissions() {
        Map<JsonPointer, Set<Permission>> attributePermissionMap = new HashMap<JsonPointer, Set<Permission>>();
            //TODO: What should be the permission set? Just read or CRUD?
        attributePermissionMap.put(JsonPointer.valueOf(""), ImmutableSet.of(READ));
        return attributePermissionMap;
    }

    @Override
    @Transactional
    public void revokeAccess(String jurisdictionId, String caseTypeId, String caseReference, Long caseId, String userId, String caseRole) {
        ExplicitAccessMetadata explicitAccessMetadata =
            ExplicitAccessMetadata.builder()
                .accessorId(userId)
                .accessorType(AccessorType.USER)
                .attribute(JsonPointer.valueOf(""))
                .relationship(caseRole)
                .resourceId(caseId.toString())
                .serviceName(jurisdictionId)
                .resourceName(caseReference)
                .resourceType(CASE_CONSTANT)
                .build();

        accessManagementService.revokeResourceAccess(explicitAccessMetadata);
    }

    @Override
    @Transactional
    public List<Long> findCasesUserIdHasAccessTo(final String userId) {
        UserCasesEnvelope userCasesEnvelope = accessManagementService.returnUserCases(userId);
        return userCasesEnvelope.getCases().stream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<String> findCaseRoles(final String caseTypeId, final Long caseId, final String userId) {
        UserCaseRolesEnvelope envelope = accessManagementService.returnUserCaseRoles(caseId.toString(), userId);
        return envelope.getRoles();
    }
}
