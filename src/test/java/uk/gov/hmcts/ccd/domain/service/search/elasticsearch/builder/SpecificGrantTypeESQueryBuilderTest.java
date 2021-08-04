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

class SpecificGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private SpecificGrantTypeESQueryBuilder specificGrantTypeESQueryBuilder;

    @BeforeEach
    void setUp() {
        specificGrantTypeESQueryBuilder = new SpecificGrantTypeESQueryBuilder();
    }

    @Test
    void mustIncludeJurisdictionCaseIdAndClassificationInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "PRIVATE", "Test", "", "", null, "caseId1");

        BoolQueryBuilder queryBuilder = specificGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(specificRoleAssignment));

        assertNotNull(queryBuilder);
        assertTrue(queryBuilder.hasClauses());
        assertEquals(3, queryBuilder.must().size());
    }

    @Test
    void mustIncludeJurisdictionAndCaseIdInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "", "Test", "", "", null, "caseId1");

        BoolQueryBuilder queryBuilder = specificGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(specificRoleAssignment));

        assertNotNull(queryBuilder);
        assertTrue(queryBuilder.hasClauses());
        assertEquals(2, queryBuilder.must().size());
    }

    @Test
    void mustIncludeJurisdictionInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "", "Test", "", "", null, "");

        BoolQueryBuilder queryBuilder = specificGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(specificRoleAssignment));

        assertNotNull(queryBuilder);
        assertTrue(queryBuilder.hasClauses());
        assertEquals(1, queryBuilder.must().size());
    }

    @Test
    void mustNotIncludeConditionsInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "", "", "", "", null, "");

        BoolQueryBuilder queryBuilder = specificGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(specificRoleAssignment));

        assertNotNull(queryBuilder);
        assertFalse(queryBuilder.hasClauses());
        assertEquals(0, queryBuilder.must().size());
    }
}
