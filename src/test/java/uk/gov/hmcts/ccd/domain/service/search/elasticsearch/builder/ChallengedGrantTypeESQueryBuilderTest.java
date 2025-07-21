package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ChallengedGrantTypeESQueryBuilderTest extends GrantTypeESQueryBuilderTest {

    private ChallengedGrantTypeESQueryBuilder challengedGrantTypeESQueryBuilder;

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
        challengedGrantTypeESQueryBuilder =
            new ChallengedGrantTypeESQueryBuilder(accessControlService, caseDataAccessControl, applicationParams);
    }

    @Test
    void shouldNotReturnQueryWhenChallengedGrantTypeNotPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(
            GrantType.STANDARD, "CASE", "PRIVATE", "", "", null
        );
        Query query = challengedGrantTypeESQueryBuilder
            .createQuery(List.of(roleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypePresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(
            GrantType.CHALLENGED, "CASE", "PRIVATE", "TEST", "", "", null
        );
        Query query = challengedGrantTypeESQueryBuilder
            .createQuery(List.of(roleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypeWithNoJurisdictionPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(
            GrantType.CHALLENGED, "CASE", "PRIVATE", "", "", "", null
        );
        Query query = challengedGrantTypeESQueryBuilder
            .createQuery(List.of(roleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypeWithNoJurisdictionNoClassificationPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(
            GrantType.CHALLENGED, "CASE", "", "", "", "", null
        );
        Query query = challengedGrantTypeESQueryBuilder
            .createQuery(List.of(roleAssignment), caseTypeDefinition);

        assertNotNull(query);
    }
}
