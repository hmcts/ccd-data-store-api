package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BasicGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private BasicGrantTypeESQueryBuilder basicGrantTypeESQueryBuilder;

    @BeforeEach
    void setUp() {
        basicGrantTypeESQueryBuilder = new BasicGrantTypeESQueryBuilder();
    }

    @Test
    void shouldIncludeMustQueryWhenClassificationPresentInRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "PRIVATE", "", "", null);
        BoolQueryBuilder query = basicGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment));
        assertNotNull(query);
    }


    @Test
    void shouldNotIncludeMustQueryWhenClassificationPresentInRoleAssignment() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "", "", "", null);
        BoolQueryBuilder query = basicGrantTypeESQueryBuilder.createQuery(Lists.newArrayList(roleAssignment));
        assertNotNull(query);
    }
}
