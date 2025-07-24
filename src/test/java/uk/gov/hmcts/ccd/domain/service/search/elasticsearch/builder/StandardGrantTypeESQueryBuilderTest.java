package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class StandardGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private StandardGrantTypeESQueryBuilder standardGrantTypeESQueryBuilder;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseTypeDefinition caseTypeDefinition;

    @BeforeEach
    void setUp() {
        standardGrantTypeESQueryBuilder =
            new StandardGrantTypeESQueryBuilder(accessControlService, caseDataAccessControl, applicationParams);
    }

    @Test
    void shouldCreateQueryWithAllParameters() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(
            GrantType.STANDARD, "CASE", "PRIVATE", "Test", "loc1", "reg1", null, "caseId1"
        );

        Query query = standardGrantTypeESQueryBuilder
            .createQuery(List.of(standardRoleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void shouldCreateQueryWithoutLocation() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(
            GrantType.STANDARD, "CASE", "PRIVATE", "Test", "", "reg1", null, "caseId1"
        );

        Query query = standardGrantTypeESQueryBuilder
            .createQuery(List.of(standardRoleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void shouldCreateQueryWithoutRegion() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(
            GrantType.STANDARD, "CASE", "PRIVATE", "Test", "loc1", "", null, "caseId1"
        );

        Query query = standardGrantTypeESQueryBuilder
            .createQuery(List.of(standardRoleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void shouldCreateQueryWithJurisdiction() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(
            GrantType.STANDARD, "CASE", "PRIVATE", "Test", "", "", null, "caseId1"
        );

        Query query = standardGrantTypeESQueryBuilder
            .createQuery(List.of(standardRoleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void shouldNotCreateInnerQuery() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(
            GrantType.STANDARD, "CASE", "PRIVATE", "", "", "", null, "caseId1"
        );

        Query query = standardGrantTypeESQueryBuilder
            .createQuery(List.of(standardRoleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void shouldCreateQueryWithJurisdictionWhenLocationNull() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(
            GrantType.STANDARD, "CASE", "PRIVATE", "Test", null, "", null, "caseId1"
        );

        Query query = standardGrantTypeESQueryBuilder
            .createQuery(List.of(standardRoleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }
}
