package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(queryBuilder.hasClauses());
        assertEquals(1, queryBuilder.must().size());
        assertEquals(1, queryBuilder.should().size());
        BoolQueryBuilder innerQuery = (BoolQueryBuilder) queryBuilder.should().get(0);
        assertEquals(3, innerQuery.must().size());
    }

    @Test
    void shouldCreateQueryWithoutLocation() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "Test", "", "reg1", null, "caseId1");

        BoolQueryBuilder queryBuilder = standardGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(standardRoleAssignment));

        assertNotNull(queryBuilder);
        assertTrue(queryBuilder.hasClauses());
        assertEquals(1, queryBuilder.must().size());
        assertEquals(1, queryBuilder.should().size());
        BoolQueryBuilder innerQuery = (BoolQueryBuilder) queryBuilder.should().get(0);
        assertEquals(2, innerQuery.must().size());
    }

    @Test
    void shouldCreateQueryWithoutRegion() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "Test", "loc1", "", null, "caseId1");

        BoolQueryBuilder queryBuilder = standardGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(standardRoleAssignment));

        assertNotNull(queryBuilder);
        assertTrue(queryBuilder.hasClauses());
        assertEquals(1, queryBuilder.must().size());
        assertEquals(1, queryBuilder.should().size());
        BoolQueryBuilder innerQuery = (BoolQueryBuilder) queryBuilder.should().get(0);
        assertEquals(2, innerQuery.must().size());
    }

    @Test
    void shouldCreateQueryWithJurisdiction() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "Test", "", "", null, "caseId1");

        BoolQueryBuilder queryBuilder = standardGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(standardRoleAssignment));

        assertNotNull(queryBuilder);
        assertTrue(queryBuilder.hasClauses());
        assertEquals(1, queryBuilder.must().size());
        assertEquals(1, queryBuilder.should().size());
        BoolQueryBuilder innerQuery = (BoolQueryBuilder) queryBuilder.should().get(0);
        assertEquals(1, innerQuery.must().size());
    }

    @Test
    void shouldNotCreateInnerQuery() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "", "", "", null, "caseId1");

        BoolQueryBuilder queryBuilder = standardGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(standardRoleAssignment));

        assertNotNull(queryBuilder);
        assertTrue(queryBuilder.hasClauses());
        assertEquals(1, queryBuilder.must().size());
        assertEquals(1, queryBuilder.should().size());
        BoolQueryBuilder innerQuery = (BoolQueryBuilder) queryBuilder.should().get(0);
        assertFalse(innerQuery.hasClauses());
    }

    @Test
    void shouldCreateQueryWithJurisdictionWhenLocationNull() {
        RoleAssignment standardRoleAssignment = createRoleAssignment(GrantType.STANDARD,
            "CASE", "PRIVATE", "Test", null, "", null, "caseId1");

        BoolQueryBuilder queryBuilder = standardGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(standardRoleAssignment));

        assertNotNull(queryBuilder);
        assertTrue(queryBuilder.hasClauses());
        assertEquals(1, queryBuilder.must().size());
        assertEquals(1, queryBuilder.should().size());
        BoolQueryBuilder innerQuery = (BoolQueryBuilder) queryBuilder.should().get(0);
        assertEquals(1, innerQuery.must().size());
    }

}
