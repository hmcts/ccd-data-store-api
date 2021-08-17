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
        List<TermsQueryBuilder> query = challengedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
        assertTrue(query.isEmpty());
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypePresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED, "CASE",
            "PRIVATE",  "TEST", "", "", null);
        List<TermsQueryBuilder> query = challengedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
        assertEquals(2, query.size());
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypeWithNoJurisdictionPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED, "CASE",
            "PRIVATE",  "", "", "", null);
        List<TermsQueryBuilder> query = challengedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
        assertEquals(1, query.size());
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypeWithNoJurisdictionNoClassificationPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED, "CASE",
            "",  "", "", "", null);
        List<TermsQueryBuilder> query = challengedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
        assertEquals(0, query.size());
    }
}
