package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExcludedGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private ExcludedGrantTypeESQueryBuilder excludedGrantTypeESQueryBuilder;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseTypeDefinition caseTypeDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        excludedGrantTypeESQueryBuilder =
            new ExcludedGrantTypeESQueryBuilder(accessControlService, caseDataAccessControl, applicationParams);
    }

    @Test
    void shouldReturnQueryWhenExcludedGrantTypePresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(
            GrantType.EXCLUDED,
            "CASE",
            "PRIVATE",
            "TEST",
            "", "", null, "123"
        );

        Query query = excludedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void shouldReturnQueryWhenExcludedGrantTypeNoCaseIdPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(
            GrantType.EXCLUDED,
            "CASE",
            "PRIVATE",
            "TEST",
            "", "", null
        );

        Query query = excludedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void shouldReturnEmptyQueryWhenExcludedGrantTypeNoCaseIdNoClassificationPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(
            GrantType.EXCLUDED,
            "CASE",
            "",
            "TEST",
            "", "", null
        );

        Query query = excludedGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }
}
