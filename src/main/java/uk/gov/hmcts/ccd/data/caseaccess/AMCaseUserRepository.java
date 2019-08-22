package uk.gov.hmcts.ccd.data.caseaccess;

import com.fasterxml.jackson.core.JsonPointer;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.amlib.enums.Permission.READ;

@Named
@Singleton
@Qualifier(AMCaseUserRepository.QUALIFIER)
public class AMCaseUserRepository implements CaseUserRepository {

    public static final String QUALIFIER = "am";
    private static final String cases = "case";

    @Autowired
    AccessManagementService accessManagementService;

    @Override
    public void grantAccess(String jurisdictionId, String caseReference, Long caseId, String userId, String caseRole) {

        Set<String> accessorIds = Collections.singleton(userId);

        ResourceDefinition resourceDefinition =
            //TODO: What should be the resourceType and resourceName.
            //To be clarified with Mutlu/Shashank again. resource name: CMC, FPL
            new ResourceDefinition(jurisdictionId, cases, caseReference);

        ExplicitAccessGrant explicitAccessGrant = ExplicitAccessGrant.builder()
            .accessorType(AccessorType.USER)
            .accessorIds(accessorIds)
            .resourceId(caseId.toString())
            .attributePermissions(getAttributePermissions())
            .resourceDefinition(resourceDefinition)
            .relationship(caseRole)
            .build();

        accessManagementService.grantExplicitResourceAccess(explicitAccessGrant);
    }

    private Map<JsonPointer, Set<Permission>> getAttributePermissions() {
        return new HashMap<JsonPointer, Set<Permission>>() {{
            //TODO: What should be the permission set? Just read or CRUD?
            put(JsonPointer.valueOf(""), ImmutableSet.of(READ));
        }};
    }

    @Override
    public void revokeAccess(String jurisdictionId, String caseReference, Long caseId, String userId, String caseRole) {

        ExplicitAccessMetadata explicitAccessMetadata =
            ExplicitAccessMetadata.builder()
                .accessorId(userId)
                .accessorType(AccessorType.USER)
                .attribute(JsonPointer.valueOf(""))
                .relationship(caseRole)
                .resourceId(caseId.toString())
                .serviceName(jurisdictionId)
                .resourceName(caseReference)
                .resourceType(cases)
                .build();

        accessManagementService.revokeResourceAccess(explicitAccessMetadata);
    }

    @Override
    public List<Long> findCasesUserIdHasAccessTo(final String userId) {
        UserCasesEnvelope userCasesEnvelope =
            accessManagementService.returnUserCases(userId);
        return userCasesEnvelope.getCases().stream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public List<String> findCaseRoles(final String caseTypeId, final Long caseId, final String userId) {
        UserCaseRolesEnvelope envelope = accessManagementService.returnUserCaseRoles(caseId.toString(), userId);
        return envelope.getRoles();
    }
}
