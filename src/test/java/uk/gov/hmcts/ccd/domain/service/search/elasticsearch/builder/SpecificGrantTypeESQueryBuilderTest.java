package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import java.util.List;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

        List<TermsQueryBuilder> queryBuilder = specificGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(specificRoleAssignment));

        assertNotNull(queryBuilder);
        assertEquals(3, queryBuilder.size());
    }

    @Test
    void mustIncludeJurisdictionAndCaseIdInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "", "Test", "", "", null, "caseId1");

        List<TermsQueryBuilder> queryBuilder = specificGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(specificRoleAssignment));

        assertNotNull(queryBuilder);
        assertEquals(2, queryBuilder.size());
    }

    @Test
    void mustIncludeJurisdictionInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "", "Test", "", "", null, "");

        List<TermsQueryBuilder> queryBuilder = specificGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(specificRoleAssignment));

        assertNotNull(queryBuilder);
        assertEquals(1, queryBuilder.size());
    }

    @Test
    void mustNotIncludeConditionsInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "", "", "", "", null, "");

        List<TermsQueryBuilder> queryBuilder = specificGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(specificRoleAssignment));

        assertNotNull(queryBuilder);
        assertEquals(0, queryBuilder.size());
    }
}
