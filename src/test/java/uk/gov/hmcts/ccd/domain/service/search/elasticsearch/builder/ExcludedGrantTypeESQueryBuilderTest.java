package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

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
        BoolQueryBuilder query = excludedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
    }

    @Test
    void shouldReturnQueryWhenExcludedGrantTypeNoCaseIdPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.EXCLUDED, "CASE",
            "PRIVATE",  "TEST", "", "", null);
        BoolQueryBuilder query = excludedGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
    }

    @Test
    void shouldReturnEmptyQueryWhenExcludedGrantTypeNoCaseIdNoClassificationPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.EXCLUDED, "CASE",
            "",  "TEST", "", "", null);
        BoolQueryBuilder query = excludedGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment));

        assertNotNull(query);
    }
}
