package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

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
            () -> assertEquals(1, accessTypes.size()),
            () -> assertEquals("default", accessTypes.get(0).getAccessTypeId()),
            () -> assertEquals("SOLICITOR_PROFILE", accessTypes.get(0).getOrganisationProfileId())
        );
    }

    @Test
    @DisplayName("Get accessTypeRoles")
    void accessTypeRoles() {
        List<AccessTypeDefinition> accessTypeRoles = caseTypeDefinition.getAccessTypeDefinitions();
        assertAll(
            () -> assertEquals(1, accessTypeRoles.size()),
            () -> assertEquals("default", accessTypeRoles.get(0).getAccessTypeId()),
            () -> assertEquals("SOLICITOR_PROFILE", accessTypeRoles.get(0).getOrganisationProfileId())
        );
    }

    private void setupWithAccessType() {
        List<AccessTypeDefinition> accessTypes = new ArrayList<>();
        accessTypes.add(
            AccessTypeDefinition.builder()
                .liveFrom(LocalDate.now())
                .liveTo(LocalDate.now())
                .accessTypeId("default")
                .accessDefault(true)
                .accessMandatory(true)
                .description("description test")
                .display(true)
                .displayOrder(10)
                .hint("hint")
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
                .liveFrom(LocalDate.now())
                .liveTo(LocalDate.now())
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

    @Test
    void testAccessTypeRoleDefinitionCreateCopy() {
        AccessTypeRoleDefinition p1 = new AccessTypeRoleDefinition();

        Map<AccessTypeRoleDefinition, String> map = new HashMap<>();
        map.put(p1, "dummy");

        AccessTypeRoleDefinition p2 = p1.createCopy();
        assertEquals("dummy", map.get(p2));
    }

    @Test
    void testAccessTypeDefinitionCreateCopy() {
        AccessTypeDefinition p1 = new AccessTypeDefinition();
        Map<AccessTypeDefinition, String> map = new HashMap<>();
        map.put(p1, "dummy");
        AccessTypeDefinition p2 = p1.createCopy();
        assertEquals("dummy", map.get(p2));
    }
}
