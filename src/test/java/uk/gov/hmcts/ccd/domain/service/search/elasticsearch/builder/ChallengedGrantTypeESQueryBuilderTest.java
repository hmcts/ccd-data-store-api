package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChallengedGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private ChallengedGrantTypeESQueryBuilder challengedGrantTypeESQueryBuilder;

    @Mock
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        challengedGrantTypeESQueryBuilder = new ChallengedGrantTypeESQueryBuilder(accessControlService);
    }

    @Test
    void shouldNotReturnQueryWhenChallengedGrantTypeNotPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.STANDARD, "CASE",
            "PRIVATE", "", "", null);
        BoolQueryBuilder query = challengedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Lists.newArrayList());

        assertNotNull(query);
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypePresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED, "CASE",
            "PRIVATE",  "TEST", "", "", null);
        BoolQueryBuilder query = challengedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Lists.newArrayList());

        assertNotNull(query);
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypeWithNoJurisdictionPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED, "CASE",
            "PRIVATE",  "", "", "", null);
        BoolQueryBuilder query = challengedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Lists.newArrayList());

        assertNotNull(query);
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypeWithNoJurisdictionNoClassificationPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED, "CASE",
            "",  "", "", "", null);
        BoolQueryBuilder query = challengedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Lists.newArrayList());

        assertNotNull(query);
    }
}
