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

class ExcludedGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private ExcludedGrantTypeESQueryBuilder excludedGrantTypeESQueryBuilder;

    @BeforeEach
    void setUp() {
        excludedGrantTypeESQueryBuilder = new ExcludedGrantTypeESQueryBuilder();
    }

    @Test
    void shouldReturnQueryWhenExcludedGrantTypePresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.EXCLUDED, "CASE",
            "PRIVATE",  "TEST", "", "", null, "123");
        List<TermsQueryBuilder> query = excludedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
        assertEquals(2, query.size());
    }

    @Test
    void shouldReturnQueryWhenExcludedGrantTypeNoCaseIdPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.EXCLUDED, "CASE",
            "PRIVATE",  "TEST", "", "", null);
        List<TermsQueryBuilder> query = excludedGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
        assertEquals(1, query.size());
    }

    @Test
    void shouldReturnEmptyQueryWhenExcludedGrantTypeNoCaseIdNoClassificationPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.EXCLUDED, "CASE",
            "",  "TEST", "", "", null);
        List<TermsQueryBuilder> query = excludedGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
        assertEquals(0, query.size());
    }
}
