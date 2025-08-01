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
class SpecificGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private SpecificGrantTypeESQueryBuilder specificGrantTypeESQueryBuilder;

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
        specificGrantTypeESQueryBuilder =
            new SpecificGrantTypeESQueryBuilder(accessControlService, caseDataAccessControl, applicationParams);
    }

    @Test
    void mustIncludeJurisdictionCaseIdAndClassificationInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(
            GrantType.SPECIFIC, "CASE", "PRIVATE", "Test", "", "", null, "caseId1"
        );

        Query query = specificGrantTypeESQueryBuilder
            .createQuery(List.of(specificRoleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void mustIncludeJurisdictionAndCaseIdInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(
            GrantType.SPECIFIC, "CASE", "", "Test", "", "", null, "caseId1"
        );

        Query query = specificGrantTypeESQueryBuilder
            .createQuery(List.of(specificRoleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void mustIncludeJurisdictionInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(
            GrantType.SPECIFIC, "CASE", "", "Test", "", "", null, ""
        );

        Query query = specificGrantTypeESQueryBuilder
            .createQuery(List.of(specificRoleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void mustNotIncludeConditionsInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(
            GrantType.SPECIFIC, "CASE", "", "", "", "", null, ""
        );

        Query query = specificGrantTypeESQueryBuilder
            .createQuery(List.of(specificRoleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }
}
