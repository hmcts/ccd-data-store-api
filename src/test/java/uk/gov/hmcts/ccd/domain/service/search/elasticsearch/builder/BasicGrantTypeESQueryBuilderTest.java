package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import java.util.List;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private BasicGrantTypeESQueryBuilder basicGrantTypeESQueryBuilder;

    @BeforeEach
    void setUp() {
        basicGrantTypeESQueryBuilder = new BasicGrantTypeESQueryBuilder();
    }

    @Test
    void shouldIncludeMustQueryWhenClassificationPresentInRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "", "", null);
        List<TermsQueryBuilder> query = basicGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment));
        assertNotNull(query);
        assertFalse(query.isEmpty());
    }


    @Test
    void shouldNotIncludeMustQueryWhenClassificationPresentInRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "", "", "", null);
        List<TermsQueryBuilder> query = basicGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment));
        assertNotNull(query);
        assertTrue(query.isEmpty());
    }
}
