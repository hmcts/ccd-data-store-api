package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class GrantTypeESQueryBuilderTest {

    protected static final RoleAssignment createRoleAssignment(GrantType grantType,
                                                               String roleType,
                                                               String classification,
                                                               String location,
                                                               String region,
                                                               List<String> autorisations) {
        return createRoleAssignment(grantType, roleType, classification, "", location, region, autorisations);
    }

    protected static final RoleAssignment createRoleAssignment(GrantType grantType,
                                                               String roleType,
                                                               String classification,
                                                               String jurisdiction,
                                                               String location,
                                                               String region,
                                                               List<String> autorisations) {
        return createRoleAssignment(grantType, roleType, classification,
            jurisdiction, location, region, autorisations, "");
    }

    protected static final RoleAssignment createRoleAssignment(GrantType grantType,
                                                               String roleType,
                                                               String classification,
                                                               String jurisdiction,
                                                               String location,
                                                               String region,
                                                               List<String> autorisations,
                                                               String caseId) {
        return createRoleAssignment(grantType, roleType, classification, jurisdiction,
            location, region, autorisations, caseId, null, null);
    }

    protected static final RoleAssignment createRoleAssignment(GrantType grantType,
                                                               String roleType,
                                                               String classification,
                                                               String jurisdiction,
                                                               String location,
                                                               String region,
                                                               List<String> autorisations,
                                                               String caseId,
                                                               String roleName,
                                                               String caseAccessGroupId) {
        RoleAssignmentAttributes attributes = RoleAssignmentAttributes.builder()
            .jurisdiction(Optional.ofNullable(jurisdiction))
            .location(Optional.ofNullable(location))
            .region(Optional.ofNullable(region))
            .caseId(Optional.ofNullable(caseId))
            .caseAccessGroupId(Optional.ofNullable(caseAccessGroupId))
            .build();
        return RoleAssignment.builder()
            .grantType(grantType.name())
            .roleName(roleName)
            .authorisations(autorisations)
            .roleType(roleType)
            .classification(classification)
            .attributes(attributes)
            .build();
    }

    protected List<RoleToAccessProfileDefinition> mockRoleToAccessProfileDefinitions(String roleName,
                                                                                     String caseTypeId,
                                                                                     int numberOfAccessProfiles,
                                                                                     boolean disabled,
                                                                                     List<String> authorisations,
                                                                                     String caseAccessCategories) {
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = new ArrayList<>();
        for (int i = 0;  i < numberOfAccessProfiles; i++) {
            RoleToAccessProfileDefinition roleToAccessProfileDefinition = mock(RoleToAccessProfileDefinition.class);
            when(roleToAccessProfileDefinition.getDisabled()).thenReturn(disabled);
            when(roleToAccessProfileDefinition.getReadOnly()).thenReturn(false);
            when(roleToAccessProfileDefinition.getCaseTypeId()).thenReturn(caseTypeId);
            when(roleToAccessProfileDefinition.getRoleName()).thenReturn(roleName);
            when(roleToAccessProfileDefinition.getAccessProfileList()).thenReturn(Lists.newArrayList());
            when(roleToAccessProfileDefinition.getAuthorisationList()).thenReturn(authorisations);
            when(roleToAccessProfileDefinition.getCaseAccessCategories()).thenReturn(caseAccessCategories);
            roleToAccessProfileDefinitions.add(roleToAccessProfileDefinition);
        }
        return roleToAccessProfileDefinitions;
    }
}
