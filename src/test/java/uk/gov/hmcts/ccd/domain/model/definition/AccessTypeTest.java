package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class AccessTypeTest {

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    private CaseTypeDefinition caseTypeDefinition = mock(CaseTypeDefinition.class);

    @BeforeEach
    void setUp() {

        MockitoAnnotations.initMocks(this);
        setupWithAccessType();
        setupWithAccessRoleType();

    }

    @Test
    @DisplayName("Get accessTypes")
    void accessTypes() {
        List<AccessTypeDefinition> accessTypes = caseTypeDefinition.getAccessTypeDefinitions();
        assertAll(
            () -> assertEquals(accessTypes.size(), 1),
            () -> assertEquals(accessTypes.get(0).getAccessTypeId(), "default"),
            () -> assertEquals(accessTypes.get(0).getOrganisationProfileId(), "SOLICITOR_PROFILE")
        );
    }

    @Test
    @DisplayName("Get accessTypeRoles")
    void accessTypeRoles() {
        List<AccessTypeDefinition> accessTypeRoles = caseTypeDefinition.getAccessTypeDefinitions();
        assertAll(
            () -> assertEquals(accessTypeRoles.size(), 1),
            () -> assertEquals(accessTypeRoles.get(0).getAccessTypeId(), "default"),
            () -> assertEquals(accessTypeRoles.get(0).getOrganisationProfileId(), "SOLICITOR_PROFILE")
        );
    }

    private void setupWithAccessType() {
        List<AccessTypeDefinition> accessTypes = new ArrayList<>();
        accessTypes.add(
            AccessTypeDefinition.builder()
                .accessTypeId("default")
                .accessDefault(true)
                .accessMandatory(true)
                .description("description test")
                .display(true)
                .displayOrder(10)
                .organisationProfileId("SOLICITOR_PROFILE")
                .build());
        caseTypeDefinition.setAccessTypeDefinitions(accessTypes);

        doReturn(caseTypeDefinition)
            .when(caseDefinitionRepository).getCaseType(anyString());
        doReturn(accessTypes)
            .when(caseTypeDefinition).getAccessTypeDefinitions();
    }

    private void setupWithAccessRoleType() {
        AccessTypeRoleDefinition accessTypeRole = new AccessTypeRoleDefinition();
        accessTypeRole.setCaseAccessGroupIdTemplate("CaseAccessGroupIdTemplate");
        List<AccessTypeRoleDefinition> accessTypeRoles = new ArrayList<>();
        accessTypeRoles.add(
            AccessTypeRoleDefinition.builder()
                .accessTypeId("default")
                .groupRoleName("groupRoleName")
                .organisationalRoleName("OrgRoleName")
                .caseAccessGroupIdTemplate("$ORGID")
                .groupAccessEnabled(true)
                .caseAssignedRoleField("caseAssignedRole")
                .organisationProfileId("SOLICITOR_PROFILE")
                .build());

        caseTypeDefinition.setAccessTypeRoleDefinitions(accessTypeRoles);

        doReturn(caseTypeDefinition)
            .when(caseDefinitionRepository).getCaseType(anyString());
        doReturn(accessTypeRoles)
            .when(caseTypeDefinition).getAccessTypeRoleDefinitions();
    }

}
