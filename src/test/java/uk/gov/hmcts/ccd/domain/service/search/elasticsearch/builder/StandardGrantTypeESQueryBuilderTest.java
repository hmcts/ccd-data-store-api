package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class StandardGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private StandardGrantTypeESQueryBuilder standardGrantTypeESQueryBuilder;

    @BeforeEach
    void setUp() {
        standardGrantTypeESQueryBuilder = new StandardGrantTypeESQueryBuilder();
    }

    @Test
    void shouldCreateQueryWithAllParameters() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "Test", "loc1", "reg1", null, "caseId1");

        BoolQueryBuilder queryBuilder = standardGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(standardRoleAssignment));

        assertNotNull(queryBuilder);
    }

    @Test
    void shouldCreateQueryWithoutLocation() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "Test", "", "reg1", null, "caseId1");

        BoolQueryBuilder queryBuilder = standardGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(standardRoleAssignment));

        assertNotNull(queryBuilder);
    }

    @Test
    void shouldCreateQueryWithoutRegion() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "Test", "loc1", "", null, "caseId1");

        BoolQueryBuilder queryBuilder = standardGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(standardRoleAssignment));

        assertNotNull(queryBuilder);
    }

    @Test
    void shouldCreateQueryWithJurisdiction() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "Test", "", "", null, "caseId1");

        BoolQueryBuilder queryBuilder = standardGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(standardRoleAssignment));

        assertNotNull(queryBuilder);
    }

    @Test
    void shouldNotCreateInnerQuery() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "", "", "", null, "caseId1");

        BoolQueryBuilder queryBuilder = standardGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(standardRoleAssignment));

        assertNotNull(queryBuilder);
    }

    @Test
    void shouldCreateQueryWithJurisdictionWhenLocationNull() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "Test", null, "", null, "caseId1");

        BoolQueryBuilder queryBuilder = standardGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(standardRoleAssignment));

        assertNotNull(queryBuilder);
    }

}
