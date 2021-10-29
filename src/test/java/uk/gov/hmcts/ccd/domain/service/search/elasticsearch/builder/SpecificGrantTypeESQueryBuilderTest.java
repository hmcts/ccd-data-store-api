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

class SpecificGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private SpecificGrantTypeESQueryBuilder specificGrantTypeESQueryBuilder;

    @Mock
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        specificGrantTypeESQueryBuilder = new SpecificGrantTypeESQueryBuilder(accessControlService);
    }

    @Test
    void mustIncludeJurisdictionCaseIdAndClassificationInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "PRIVATE", "Test", "", "", null, "caseId1");

        BoolQueryBuilder queryBuilder = specificGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(specificRoleAssignment), Lists.newArrayList());

        assertNotNull(queryBuilder);
    }

    @Test
    void mustIncludeJurisdictionAndCaseIdInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "", "Test", "", "", null, "caseId1");

        BoolQueryBuilder queryBuilder = specificGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(specificRoleAssignment), Lists.newArrayList());

        assertNotNull(queryBuilder);
    }

    @Test
    void mustIncludeJurisdictionInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "", "Test", "", "", null, "");

        BoolQueryBuilder queryBuilder = specificGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(specificRoleAssignment), Lists.newArrayList());

        assertNotNull(queryBuilder);
    }

    @Test
    void mustNotIncludeConditionsInQuery() {
        RoleAssignment specificRoleAssignment = createRoleAssignment(GrantType.SPECIFIC,
            "CASE", "", "", "", "", null, "");

        BoolQueryBuilder queryBuilder = specificGrantTypeESQueryBuilder
            .createQuery(Lists.newArrayList(specificRoleAssignment), Lists.newArrayList());

        assertNotNull(queryBuilder);
    }
}
