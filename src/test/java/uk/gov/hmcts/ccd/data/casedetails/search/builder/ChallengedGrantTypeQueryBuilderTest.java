package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.data.casedetails.search.builder.SqlParamAssert.assertBoundParams;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChallengedGrantTypeQueryBuilderTest extends GrantTypeQueryBuilderTest {

    private ChallengedGrantTypeQueryBuilder challengedGrantTypeQueryBuilder;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        params = Maps.newHashMap();
        challengedGrantTypeQueryBuilder = new ChallengedGrantTypeQueryBuilder(accessControlService,
            caseDataAccessControl, applicationParams);

        CaseStateDefinition caseStateDefinition = mock(CaseStateDefinition.class);
        when(caseStateDefinition.getId()).thenReturn("CaseCreated");
        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(Lists.newArrayList(caseStateDefinition));
    }

    @Test
    void shouldNotReturnQueryWhenChallengedGrantTypeNotPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.STANDARD, "CASE",
            "ROLE1", "PRIVATE", "", "", null);
        String query = challengedGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), params, null);

        assertNotNull(query);
        assertEquals("", query);
        assertTrue(params.isEmpty(), "an empty query must bind nothing, params were: " + params);
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypePresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED, "CASE",
                "ROLE1", "PRIVATE",  "TEST", "", "", null);
        String query = challengedGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), params, null);

        assertNotNull(query);
        String expectedValue = "( jurisdiction = :abac$jurisdiction_1_challenged "
            + "AND state in (:abac$states_1_challenged) "
            + "AND security_classification in (:abac$classifications_1_challenged) )";
        assertEquals(expectedValue, query);
        assertBoundParams(query, params,
            "abac$jurisdiction_1_challenged", "TEST",
            "abac$states_1_challenged", Set.of("CaseCreated"),
            "abac$classifications_1_challenged", List.of("PUBLIC", "PRIVATE"));
    }

    @Test
    void shouldReturnEmptyQueryWhenCaseStatesNotPresent() {
        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(Lists.newArrayList());
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED, "CASE",
            "ROLE1", "PRIVATE",  "TEST", "", "", null);
        String query = challengedGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), params, null);

        assertNotNull(query);
        assertEquals("", query);
        assertTrue(params.isEmpty(), "an empty query must bind nothing, params were: " + params);
    }
}
