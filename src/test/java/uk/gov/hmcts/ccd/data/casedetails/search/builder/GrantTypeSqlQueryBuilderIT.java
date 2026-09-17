package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.casedetails.search.builder.SqlParamAssert.assertParamsMatchQuery;

/**
 * Executes the clause produced by {@link GrantTypeSqlQueryBuilder} (through the concrete
 * {@link BasicGrantTypeQueryBuilder}) against the Testcontainers Postgres instance already used by
 * the integration tests, binding the params map exactly as production does through a
 * {@link NamedParameterJdbcTemplate}.
 * Each test seeds its own rows (the base class truncates {@code case_data} after every test) and
 * asserts on the exact set of returned case references, so an accidental match cannot pass as a
 * count coincidence.
 */
public class GrantTypeSqlQueryBuilderIT extends WireMockBaseTest {

    private static final String QUOTE_PAYLOAD = "x' OR '1'='1";

    private static final String CASE_TYPE_ID = "TestAddressBookCase";

    @PersistenceContext
    private EntityManager entityManager;

    private BasicGrantTypeQueryBuilder queryBuilder;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseDataAccessControl caseDataAccessControl;
    private ApplicationParams applicationParams;
    private JdbcTemplate template;

    @BeforeEach
    public void setUp() {
        template = new JdbcTemplate(db);

        final AccessControlService accessControlService = mock(AccessControlService.class);
        caseDataAccessControl = mock(CaseDataAccessControl.class);
        applicationParams = mock(ApplicationParams.class);
        caseTypeDefinition = mock(CaseTypeDefinition.class);

        CaseStateDefinition caseStateDefinition = mock(CaseStateDefinition.class);
        when(caseStateDefinition.getId()).thenReturn("CaseCreated");
        when(caseTypeDefinition.getStates()).thenReturn(Lists.newArrayList(caseStateDefinition));
        when(accessControlService.filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(Lists.newArrayList(caseStateDefinition));

        queryBuilder = new BasicGrantTypeQueryBuilder(accessControlService, caseDataAccessControl,
            applicationParams);
    }

    private RoleAssignment basicRoleAssignment(String jurisdiction, String location, String region,
                                               String caseAccessGroupId) {
        RoleAssignmentAttributes attributes = RoleAssignmentAttributes.builder()
            .jurisdiction(Optional.ofNullable(jurisdiction))
            .location(Optional.ofNullable(location))
            .region(Optional.ofNullable(region))
            .caseAccessGroupId(Optional.ofNullable(caseAccessGroupId))
            .build();
        return RoleAssignment.builder()
            .roleName("ROLE1")
            .grantType(GrantType.BASIC.name())
            .roleType("CASE")
            .classification("")
            .attributes(attributes)
            .build();
    }

    private void stubCaseAccessCategory(String category) {
        AccessProfile accessProfile = mock(AccessProfile.class);
        when(accessProfile.getAccessProfile()).thenReturn("ROLE1");
        when(accessProfile.getCaseAccessCategories()).thenReturn(category);
        when(caseDataAccessControl.filteredAccessProfiles(anyList(), any(CaseTypeDefinition.class),
            anyBoolean())).thenReturn(Sets.newHashSet(accessProfile));
    }

    private void seedCase(String reference, String jurisdiction, String dataJson) {
        template.update(
            """
                INSERT INTO case_data (case_type_id, jurisdiction, state, security_classification, data, \
                reference, created_date, last_modified) \
                VALUES (?, ?, 'CaseCreated', CAST(? AS securityclassification), CAST(? AS jsonb), ?, \
                now(), now())""",
            CASE_TYPE_ID, jurisdiction, "PUBLIC", dataJson, Long.valueOf(reference));
    }

    private String locationRegionData(String baseLocation, String region) {
        return String.format(
            """
                {"caseManagementLocation":{"baseLocation":"%s","region":"%s"}}""",
            baseLocation, region);
    }

    private String caseAccessGroupData(String caseAccessGroupId) {
        return String.format(
            """
                {"CaseAccessGroups":[{"id":"g1","value":{"caseAccessGroupId":"%s",\
                "caseAccessGroupType":"CCD:all-cases-access"}}]}""",
            caseAccessGroupId);
    }

    private String caseAccessCategoryData(String category) {
        return String.format("""
            {"CaseAccessCategory":"%s"}""", category);
    }

    private Set<String> search(List<RoleAssignment> roleAssignments) {
        Map<String, Object> params = new HashMap<>();
        String clause = queryBuilder.createQuery(roleAssignments, params, caseTypeDefinition);
        assertTrue(clause != null && !clause.isBlank(),
            "builder produced no clause - the test would prove nothing");
        assertParamsMatchQuery(clause, params);
        return execute(clause, params);
    }

    @SuppressWarnings("unchecked")
    private Set<String> execute(String clause, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery("SELECT reference FROM case_data WHERE " + clause);
        params.forEach(query::setParameter);
        return ((List<Object>) query.getResultList()).stream()
            .map(String::valueOf)
            .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("jurisdiction/region/location equality is well-formed and executes")
    void equalityAttributes_wellFormed_executeCleanly() {
        String ref = "1111000000000001";
        seedCase(ref, "PROBATE", locationRegionData("LOC-1", "GB-EAST"));

        assertDoesNotThrow(() ->
            assertEquals(Set.of(ref),
                search(Lists.newArrayList(basicRoleAssignment("PROBATE", "LOC-1", "GB-EAST",
                    null)))));
    }

    @Test
    @DisplayName("Hand-built CaseAccessGroups JSONB containment is well-formed and executes")
    void jsonbContainment_wellFormed_executesCleanly() {
        when(applicationParams.getCaseGroupAccessFilteringEnabled()).thenReturn(true);
        String ref = "1111000000000002";
        seedCase(ref, "PROBATE", caseAccessGroupData("GRP-1"));

        assertDoesNotThrow(() ->
            assertEquals(Set.of(ref),
                search(Lists.newArrayList(basicRoleAssignment(null, null, null,
                    "GRP-1")))));
    }

    @Test
    @DisplayName("caseAccessCategory LIKE with ESCAPE clause is well-formed and executes")
    void caseAccessCategoryLike_wellFormed_executesCleanly() {
        stubCaseAccessCategory("CIVIL");
        String ref = "1111000000000003";
        seedCase(ref, "PROBATE", caseAccessCategoryData("CIVIL"));

        assertDoesNotThrow(() ->
            assertEquals(Set.of(ref),
                search(Lists.newArrayList(basicRoleAssignment(null, null, null,
                    null)))));
    }

    @Test
    @DisplayName("jurisdiction+region match is returned, a differing case is not")
    void jurisdictionRegion_matchIncluded_mismatchExcluded() {
        String matchRef = "2222000000000001";
        String otherRef = "2222000000000002";
        seedCase(matchRef, "PROBATE", locationRegionData("LOC-1", "GB-EAST"));
        seedCase(otherRef, "DIVORCE", locationRegionData("LOC-2", "GB-WEST"));

        Set<String> returned =
            search(Lists.newArrayList(basicRoleAssignment("PROBATE", null, "GB-EAST",
                null)));

        assertEquals(Set.of(matchRef), returned,
            "only the matching case must be returned, returned: " + returned);
        assertFalse(returned.contains(otherRef), "the differing case must be excluded");
    }

    @Test
    @DisplayName("caseAccessGroupId containment returns the member case, not the non-member")
    void caseAccessGroupId_matchIncluded_nonMatchExcluded() {
        when(applicationParams.getCaseGroupAccessFilteringEnabled()).thenReturn(true);
        String memberRef = "2222000000000011";
        String nonMemberRef = "2222000000000012";
        seedCase(memberRef, "PROBATE", caseAccessGroupData("GRP-1"));
        seedCase(nonMemberRef, "PROBATE", caseAccessGroupData("GRP-2"));

        Set<String> returned =
            search(Lists.newArrayList(basicRoleAssignment(null, null, null,
                "GRP-1")));

        assertEquals(Set.of(memberRef), returned,
            "only the group member must be returned, returned: " + returned);
        assertFalse(returned.contains(nonMemberRef), "the non-member case must be excluded");
    }

    @Test
    @DisplayName("Two grant groups return the union - a case matching EITHER group, none matching neither")
    void multipleGrantGroups_returnUnion() {
        String probateRef = "2222000000000021";
        String divorceRef = "2222000000000022";
        String familyRef = "2222000000000023";
        seedCase(probateRef, "PROBATE", "{}");
        seedCase(divorceRef, "DIVORCE", "{}");
        seedCase(familyRef, "FAMILY", "{}");

        RoleAssignment groupOne = basicRoleAssignment("PROBATE", null, null,
            null);
        RoleAssignment groupTwo = basicRoleAssignment("DIVORCE", null, null,
            null);

        Set<String> returned = search(Lists.newArrayList(groupOne, groupTwo));

        assertEquals(Set.of(probateRef, divorceRef), returned,
            "the union of both groups must be returned, returned: " + returned);
        assertFalse(returned.contains(familyRef), "a case matching neither group must be excluded");
    }

    @Test
    @DisplayName("A quote-bearing region is bound as data - matches only the literal value, not every row")
    void quoteBearingRegion_isInert_matchesLiteralOnly() {
        String literalRef = "3333000000000001";
        String normalRef = "3333000000000002";

        seedCase(literalRef, "PROBATE", locationRegionData("LOC-1", QUOTE_PAYLOAD));
        seedCase(normalRef, "PROBATE", locationRegionData("LOC-2", "GB-EAST"));

        Set<String> returned =
            search(Lists.newArrayList(basicRoleAssignment(null, null, QUOTE_PAYLOAD,
                null)));

        assertEquals(Set.of(literalRef), returned,
            "payload must match only the literal row, returned: " + returned);
        assertFalse(returned.contains(normalRef),
            "the tautology must NOT widen the result to unrelated rows");
    }

    @Test
    @DisplayName("A well-formed jurisdiction is bound and executes end to end")
    void wellFormedJurisdiction_isBound_executesCleanly() {
        Map<String, Object> params = new HashMap<>();
        String clause = queryBuilder.createQuery(
            Lists.newArrayList(basicRoleAssignment("PROBATE", null, null,
                null)), params, caseTypeDefinition);

        assertEquals("( jurisdiction = :abac$jurisdiction_1_basic )", clause);
        assertEquals("PROBATE", params.get("abac$jurisdiction_1_basic"));
        assertParamsMatchQuery(clause, params);

        String probateRef = "4444000000000001";
        String divorceRef = "4444000000000002";
        seedCase(probateRef, "PROBATE", "{}");
        seedCase(divorceRef, "DIVORCE", "{}");

        Set<String> returned = execute(clause, params);

        assertEquals(Set.of(probateRef), returned, "returned: " + returned);
    }

    @Test
    @DisplayName("A quote-bearing jurisdiction no longer breaks the query - it is bound as a literal")
    void quoteBearingJurisdiction_isBound_executesCleanly_treatedAsLiteral() {
        Map<String, Object> params = new HashMap<>();
        String clause = queryBuilder.createQuery(
            Lists.newArrayList(basicRoleAssignment(QUOTE_PAYLOAD, null, null, null)),
            params, caseTypeDefinition);

        assertEquals("( jurisdiction = :abac$jurisdiction_1_basic )", clause);
        assertFalse(clause.contains(QUOTE_PAYLOAD), "payload must not appear in SQL text: " + clause);
        assertEquals(QUOTE_PAYLOAD, params.get("abac$jurisdiction_1_basic"));
        assertParamsMatchQuery(clause, params);

        String probateRef = "4444000000000011";
        seedCase(probateRef, "PROBATE", "{}");

        Set<String> returned = assertDoesNotThrow(() -> execute(clause, params));

        assertTrue(returned.isEmpty(),
            "the bound payload must match no rows (treated as literal data), returned: " + returned);
    }

    @Test
    @DisplayName("A search field named after the region ABAC param cannot rebind it and widen the results")
    void searchFieldSharingAbacParamName_cannotWidenResults() {
        String grantedRef = "6666000000000001";
        String otherRef = "6666000000000002";
        seedCase(grantedRef, "PROBATE", locationRegionData("LOC-1", "GB-EAST"));
        seedCase(otherRef, "PROBATE", locationRegionData("LOC-1", "GB-WEST"));

        Map<String, Object> params = new HashMap<>();
        String abacClause = queryBuilder.createQuery(
            Lists.newArrayList(basicRoleAssignment(null, null, "GB-EAST", null)),
            params, caseTypeDefinition);

        // `case.region_1_basic=GB-WEST` sanitises to the parameter name that the region predicate used to use.
        String searchField = String.format(GrantTypeSqlQueryBuilder.REGION_PARAM, 1, "basic")
            .substring(GrantTypeSqlQueryBuilder.ACCESS_CONTROL_PARAM_PREFIX.length());
        assertFalse(params.containsKey(searchField),
            "the ABAC region param must not sit in the namespace a search field can reach");

        Query query = entityManager.createNativeQuery(
            "SELECT reference FROM case_data WHERE " + abacClause
                + " AND data #>> '{caseManagementLocation,region}' = :" + searchField);
        // production binding order: access control params first, user criteria last
        params.forEach(query::setParameter);
        query.setParameter(searchField, "GB-WEST");

        @SuppressWarnings("unchecked")
        Set<String> returned = ((List<Object>) query.getResultList()).stream()
            .map(String::valueOf)
            .collect(Collectors.toSet());

        assertTrue(returned.isEmpty(),
            "the ABAC region restriction (GB-EAST) must still apply and contradict the search field (GB-WEST), "
                + "returned: " + returned);
        assertFalse(returned.contains(otherRef),
            "a case outside the granted region must never be reachable by naming a search field after an "
                + "ABAC parameter");
    }

    @Test
    @DisplayName("caseAccessCategory 'A%B' matches the literal 'A%B' case, not the wildcard-widened 'AXBC'")
    void caseAccessCategoryLike_escapesPercent_narrowsToLiteral() {
        stubCaseAccessCategory("A%B");
        String literalRef = "5555000000000001";
        String wildcardRef = "5555000000000002";

        seedCase(literalRef, "PROBATE", caseAccessCategoryData("A%B"));
        seedCase(wildcardRef, "PROBATE", caseAccessCategoryData("AXBC"));

        Set<String> returned =
            search(Lists.newArrayList(basicRoleAssignment(null, null, null,
                null)));

        assertEquals(Set.of(literalRef), returned,
            "the ESCAPE clause must make % a literal, matching only 'A%B', returned: " + returned);
        assertFalse(returned.contains(wildcardRef),
            "the wildcard-widened 'AXBC' case must be excluded");
    }
}
