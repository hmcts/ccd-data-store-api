package uk.gov.hmcts.ccd.data.caseaccess;

import com.fasterxml.jackson.core.JsonPointer;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.reform.amlib.AccessManagementService;
import uk.gov.hmcts.reform.amlib.enums.AccessorType;
import uk.gov.hmcts.reform.amlib.enums.Permission;
import uk.gov.hmcts.reform.amlib.models.ExplicitAccessGrant;
import uk.gov.hmcts.reform.amlib.models.ExplicitAccessMetadata;
import uk.gov.hmcts.reform.amlib.models.ResourceDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.reform.amlib.enums.Permission.READ;

@Named
@Singleton
@Qualifier(AMCaseUserRepository.QUALIFIER)
public class AMCaseUserRepository implements CaseUserRepository {

    public static final String QUALIFIER = "am";
    private String cases = "CASE";

    @PersistenceContext
    private EntityManager em;

    @Autowired
    AccessManagementService accessManagementService;

    @Override
    public void grantAccess(String jurisdictionId, String caseReference, Long caseId, String userId, String caseRole) {
        Set<String> accessorIds = new HashSet<String>() {{
            add(userId);
        }};

        ResourceDefinition resourceDefinition =
            //TODO: What should be the resourceType and resourceName
            //resoruce name: CMC, FPL
            new ResourceDefinition(jurisdictionId, cases, "TODO::::::");

        ExplicitAccessGrant explicitAccessGrant = ExplicitAccessGrant.builder()
            .accessorType(AccessorType.USER)
            .accessorIds(accessorIds)
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
                .resourceId(caseReference)
                .serviceName(jurisdictionId)
                .resourceName(cases)
                .resourceType(cases)
                .build();

        accessManagementService.revokeResourceAccess(explicitAccessMetadata);
    }

    @Override
    public List<Long> findCasesUserIdHasAccessTo(final String userId) {
        return Lists.newArrayList();
    }

    @Override
    public List<String> findCaseRoles(final String casecTypeId, final Long caseId, final String userId) {
        return Lists.newArrayList();
    }
}
