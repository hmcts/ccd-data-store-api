package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengedGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private ChallengedGrantTypeESQueryBuilder challengedGrantTypeESQueryBuilder;

    @BeforeEach
    void setUp() {
        challengedGrantTypeESQueryBuilder = new ChallengedGrantTypeESQueryBuilder();
    }

    @Test
    void shouldNotReturnQueryWhenChallengedGrantTypeNotPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.STANDARD, "CASE",
            "PRIVATE", "", "", null);
        BoolQueryBuilder query = challengedGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
        assertFalse(query.hasClauses());
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypePresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED, "CASE",
            "PRIVATE",  "TEST", "", "", null);
        BoolQueryBuilder query = challengedGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
        assertTrue(query.hasClauses());
        assertEquals(2, query.must().size());
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypeWithNoJurisdictionPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED, "CASE",
            "PRIVATE",  "", "", "", null);
        BoolQueryBuilder query = challengedGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
        assertTrue(query.hasClauses());
        assertEquals(1, query.must().size());
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypeWithNoJurisdictionNoClassificationPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED, "CASE",
            "",  "", "", "", null);
        BoolQueryBuilder query = challengedGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
        assertFalse(query.hasClauses());
        assertEquals(0, query.must().size());
    }
}
